package com.kapa.binance.service.impl;

import com.kapa.binance.base.utils.CalculatorUtil;
import com.kapa.binance.enums.CopyOrderTypeEnum;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.DataOrder;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.model.response.SymbolInfo;
import com.kapa.binance.service.CopierAccountService;
import com.kapa.binance.service.CopierService;
import com.kapa.binance.service.external.AccountApi;
import com.kapa.binance.service.external.OrderApi;
import com.kapa.binance.service.external.PositionApi;
import com.kapa.binance.service.external.SymbolApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopierServiceImpl implements CopierService {

    private static final String KEY_LOG = "userId";
    private final CopierAccountService copierAccountService;
    private final OrderApi orderApi;
    private final PositionApi positionApi;
    private final SymbolApi symbolApi;
    private final AccountApi accountApi;

    public static void withMDC(String value, Runnable task) {
        MDC.put(KEY_LOG, value);
        try {
            task.run();
        } finally {
            MDC.remove(KEY_LOG);
        }
    }

    @Override
    public void sendCopierOrder(DataOrder dataOrder, AuthRequest leaderAuthRequest) {
        String requestId = UUID.randomUUID().toString();
        log.info("---Start sendCopierOrder request id: {} ---", requestId);

        // 1. Detect leader order type
        final String leaderUuid = leaderAuthRequest.getUuid();
        final String side = dataOrder.getSide();
        final String orderType = dataOrder.getOrderType();
        final String positionSide = dataOrder.getPositionSide();
        final Double leaderQuantity = dataOrder.getOriginalQuantity();
        final String symbol = dataOrder.getSymbol();
        final Boolean closePosition = dataOrder.getIsCloseAll();

        final CopyOrderTypeEnum copyOrderType = CopyOrderTypeEnum.detectOrderType(
                positionSide, orderType, side, closePosition
        );

        log.info("Leader {} order detected => type: {}, side: {}, posSide: {}, orderType: {}",
                leaderUuid, copyOrderType, side, positionSide, orderType);

        if (CopyOrderTypeEnum.UNKNOWN.equals(copyOrderType)) return;

        // 2. Loop through copiers
        List<AuthRequest> copiers = copierAccountService.getCopierAuthByLeader(leaderUuid);
        log.info("Leader {} have {} copier", leaderUuid, copiers.size());
        if (copiers.isEmpty()) return;

        final PositionInfo leaderPosition;
        final SymbolInfo symbolInfo;
        if (CopyOrderTypeEnum.CLOSE_POSITION.equals(copyOrderType)) {
            leaderPosition = positionApi.getPosition(leaderAuthRequest, symbol, positionSide);
            symbolInfo = symbolApi.getSymbolInfo(symbol);
        } else {
            leaderPosition = null;
            symbolInfo = null;
        }

        Integer leaderLeverage = null;
        if (CopyOrderTypeEnum.MARKET_ORDER.equals(copyOrderType)) {
            PositionInfo p = positionApi.getPositionInfo(leaderAuthRequest, symbol, positionSide);
            if (p != null) {
                leaderLeverage = p.getLeverage();
            } else {
                log.warn("Leader {} has no position info to get leverage for symbol {} posSide {}",
                        leaderUuid, symbol, positionSide);
                return;
            }
        }

        for (AuthRequest authRequest : copiers) {
            handleCopierOrder(copyOrderType, authRequest, leaderPosition, symbolInfo,
                    symbol, positionSide, side, leaderQuantity, requestId, leaderLeverage);
        }

        log.info("---End sendCopierOrder request id: {} ---", requestId);
    }

    /**
     * Xử lý đặt lệnh cho từng copier dựa trên loại order
     */
    @Async("copierExecutor")
    public void handleCopierOrder(CopyOrderTypeEnum type, AuthRequest authRequest, PositionInfo leaderPosition,
                                   SymbolInfo symbolInfo,
                                  String symbol, String positionSide, String side, Double leaderQuantity,
                                  String requestId, Integer leaderLeverage
    ) {
        final String copierId = authRequest.getUuid();
        withMDC(copierId, () -> {
            log.info("---Start Copier request id: {} ---", requestId);
            Double copierRatio = authRequest.getCopierRatio();
            double leaderQuantityAdjusted;
            if (copierRatio == null || copierRatio <= 0) {
                leaderQuantityAdjusted = leaderQuantity * 1.0;
                log.warn("Copier uuid {} has invalid copierRatio {}, default to 1.0",
                        copierId, copierRatio);
            } else {
                leaderQuantityAdjusted = leaderQuantity * copierRatio;
                log.info("Copier uuid {} adjusted leaderQuantity {} with ratio {} => {}",
                        copierId, leaderQuantity, copierRatio, leaderQuantityAdjusted);
            }
            
            switch (type) {
                // Tang leaderQuantity
                case MARKET_ORDER:
                    updateLever(authRequest, leaderLeverage, symbol, positionSide);
                    openOrder(authRequest, symbol, positionSide, side, leaderQuantityAdjusted);
                    break;
                case LIMIT_ORDER:
                    openOrder(authRequest, symbol, positionSide, side, leaderQuantityAdjusted);
                    break;

                // Dong toan bo position
                case STOP_LOSS, TAKE_PROFIT:
                    closeAllPosition(authRequest, symbol, positionSide, side);
                    break;

                // Giam position 1 phan or toan bo
                case CLOSE_POSITION:
                    closePartialPosition(authRequest, leaderPosition, symbolInfo,
                            symbol, positionSide, side, leaderQuantityAdjusted);
                    break;

                default:
                    log.warn("UNKNOWN order type, skipping...");
            }
            log.info("---End Copier request id: {} ---", requestId);
        });
    }

    private void updateLever(AuthRequest authRequest, Integer leverage, String symbol, String posSide) {
        try {
            PositionInfo p = positionApi.getPositionInfo(authRequest, symbol, posSide);
            if (p != null && p.getPositionAmt() == 0 && (int) leverage != p.getLeverage()) {
                accountApi.changeLeverage(authRequest, symbol, leverage);
                log.info("Copier uuid {} change leverage to {} for symbol {} posSide {}",
                        authRequest.getUuid(), leverage, symbol, posSide);
            } else {
                log.info("Copier uuid {} has open position, skip change leverage for symbol {} posSide {}",
                        authRequest.getUuid(), symbol, posSide);
            }
        } catch (Exception e) {
            log.error("Copier uuid {} initLeverage Error: {}", authRequest.getUuid(), e.getMessage());
        }
    }

    private void openOrder(AuthRequest authRequest, String symbol, String posSide, String side, Double leaderQuantity) {
        try {
            orderApi.sendMarketOrder(authRequest, symbol, posSide, side, BigDecimal.valueOf(leaderQuantity));
        } catch (Exception e) {
            log.error("Copier uuid {} send openOrder Error: {}", authRequest.getUuid(), e.getMessage());
        }
    }

    private void closeAllPosition(AuthRequest authRequest, String symbol, String posSide, String side) {
        try {
            PositionInfo copierPosition = positionApi.getPosition(authRequest, symbol, posSide);
            if (copierPosition == null || copierPosition.getPositionAmt() == 0) {
                log.info("Copier uuid {} has no position to close for symbol {}", authRequest.getUuid(), symbol);
                return;
            }
            double copierQuantity = Math.abs(copierPosition.getPositionAmt());
            orderApi.closePosition(authRequest, symbol, posSide, side, BigDecimal.valueOf(copierQuantity));
        } catch (Exception e) {
            log.error("Copier uuid {} send closeAllPosition Error: {}", authRequest.getUuid(), e.getMessage());
        }
    }

    private void closePartialPosition(AuthRequest authRequest,
                                      PositionInfo leaderPosition,
                                      SymbolInfo symbolInfo,
                                      String symbol,
                                      String posSide,
                                      String side,
                                      Double leaderOrderQuantity) {
        try {
            // Lấy vị thế hiện tại của copier
            PositionInfo copierPosition = positionApi.getPosition(authRequest, symbol, posSide);
            if (copierPosition == null || copierPosition.getPositionAmt() == 0) {
                log.info("[Copier:{}] No open position to close for symbol={} posSide={}",
                        authRequest.getUuid(), symbol, posSide);
                return;
            }

            // Nếu leader không có vị thế -> copier đóng toàn bộ
            if (leaderPosition == null || leaderPosition.getPositionAmt() == 0) {
                double copierQuantity = Math.abs(copierPosition.getPositionAmt());
                log.info("[Copier:{}] Leader has no position. Closing ALL copier position: symbol={} posSide={} qty={}",
                        authRequest.getUuid(), symbol, posSide, copierQuantity);
                orderApi.closePosition(authRequest, symbol, posSide, side, BigDecimal.valueOf(copierQuantity));
                return;
            }

            // Tính toán phần trăm leader đóng
            double leaderTotal = Math.abs(leaderPosition.getPositionAmt());
            double leaderClosedPercent = (leaderOrderQuantity / leaderTotal) * 100;
            if (leaderClosedPercent > 100) {
                leaderClosedPercent = 100;
            }

            log.info("[Copier:{}] Leader closed {} units ({}% of total {} units) for symbol={} posSide={}",
                    authRequest.getUuid(), leaderOrderQuantity, leaderClosedPercent, leaderTotal, symbol, posSide);

            // Tính toán số lượng copier cần đóng
            double copierTotal = Math.abs(copierPosition.getPositionAmt());
            double copierCloseQuantity = (leaderClosedPercent / 100) * copierTotal;

            // Kiểm tra symbol info
            if (symbolInfo == null || symbolInfo.getStepSize() == 0 || symbolInfo.getQuantityPrecision() < 0) {
                log.warn("[Copier:{}] Symbol info invalid for {}. stepSize={}, precision={}. Skip closing.",
                        authRequest.getUuid(), symbol,
                        symbolInfo != null ? symbolInfo.getStepSize() : null,
                        symbolInfo != null ? symbolInfo.getQuantityPrecision() : null);
                return;
            }

            // Làm tròn số lượng
            BigDecimal quantity = CalculatorUtil.roundValue(
                    copierCloseQuantity,
                    symbolInfo.getStepSize(),
                    symbolInfo.getQuantityPrecision()
            );

            if (BigDecimal.ZERO.compareTo(quantity) <= 0) {
                log.info("[Copier:{}] Calculated close quantity <= 0. Skip closing. symbol={} posSide={}",
                        authRequest.getUuid(), symbol, posSide);
                return;
            }

            // Đặt lệnh đóng
            log.info("[Copier:{}] Closing copier partial position: symbol={} posSide={} side={} qty={} (copierTotal={})",
                    authRequest.getUuid(), symbol, posSide, side, quantity, copierTotal);

            orderApi.closePosition(authRequest, symbol, posSide, side, quantity);

        } catch (Exception e) {
            log.error("[Copier:{}] closePartialPosition Error: symbol={} posSide={} - {}",
                    authRequest.getUuid(), symbol, posSide, e.getMessage(), e);
        }
    }
}
