package com.kapa.binance.service.impl;

import com.kapa.binance.base.utils.CalculatorUtil;
import com.kapa.binance.enums.CopyOrderTypeEnum;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.DataOrder;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.model.response.SymbolInfo;
import com.kapa.binance.service.CopierAccountService;
import com.kapa.binance.service.CopierService;
import com.kapa.binance.service.external.OrderApi;
import com.kapa.binance.service.external.PositionApi;
import com.kapa.binance.service.external.SymbolApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopierServiceImpl implements CopierService {

    private final CopierAccountService copierAccountService;
    private final OrderApi orderApi;
    private final PositionApi positionApi;
    private final SymbolApi symbolApi;

    @Override
    public void sendCopierOrder(DataOrder dataOrder, AuthRequest leaderAuthRequest) {
        // 1. Detect leader order type
        String leaderUuid = leaderAuthRequest.getUuid();
        String side = dataOrder.getSide();
        String orderType = dataOrder.getOrderType();
        String positionSide = dataOrder.getPositionSide();
        Double leaderQuantity = dataOrder.getOriginalQuantity();
        String symbol = dataOrder.getSymbol();
        Boolean closePosition = dataOrder.getIsCloseAll();

        CopyOrderTypeEnum copyOrderType = CopyOrderTypeEnum.detectOrderType(
                positionSide, orderType, side, closePosition
        );

        log.info("Leader {} order detected => type: {}, side: {}, posSide: {}, orderType: {}",
                leaderUuid, copyOrderType, side, positionSide, orderType);

        if (CopyOrderTypeEnum.UNKNOWN.equals(copyOrderType)) return;

        // 2. Loop through copiers
        List<AuthRequest> copiers = copierAccountService.getCopierAuthByLeader(leaderUuid);
        log.info("Leader {} have {} copier", leaderUuid, copiers.size());
        if (copiers.isEmpty()) return;

        PositionInfo leaderPosition = null;
        SymbolInfo symbolInfo = null;
        if (CopyOrderTypeEnum.CLOSE_POSITION.equals(copyOrderType)) {
            leaderPosition = positionApi.getPosition(leaderAuthRequest, symbol, positionSide);
            symbolInfo = symbolApi.getSymbolInfo(symbol);
        }

        for (AuthRequest authRequest : copiers) {
            handleCopierOrder(copyOrderType, authRequest, leaderPosition, symbolInfo,
                    symbol, positionSide, side, leaderQuantity);
        }
    }

    /**
     * Xử lý đặt lệnh cho từng copier dựa trên loại order
     */
    private void handleCopierOrder(CopyOrderTypeEnum type, AuthRequest authRequest, PositionInfo leaderPosition,
                                   SymbolInfo symbolInfo,
                                   String symbol, String positionSide, String side, Double leaderQuantity
    ) {
        switch (type) {
            // Tang leaderQuantity
            case MARKET_ORDER, LIMIT_ORDER:
                openOrder(authRequest, symbol, positionSide, side, leaderQuantity);
                break;

            // Dong toan bo position
            case STOP_LOSS, TAKE_PROFIT:
                closeAllPosition(authRequest, symbol, positionSide, side);
                break;

            // Giam position 1 phan or toan bo
            case CLOSE_POSITION:
                closePartialPosition(authRequest, leaderPosition, symbolInfo,
                        symbol, positionSide, side, leaderQuantity);
                break;

            default:
                log.warn("UNKNOWN order type, skipping...");
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
