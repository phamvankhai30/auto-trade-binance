package com.kapa.binance.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapa.binance.base.utils.OrderMapper;
import com.kapa.binance.base.utils.StringUtil;
import com.kapa.binance.constant.CommonConstant;
import com.kapa.binance.entity.*;
import com.kapa.binance.enums.*;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.dtos.StepConfig;
import com.kapa.binance.model.response.DataOrder;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.repository.*;
import com.kapa.binance.service.CopierService;
import com.kapa.binance.service.OrderService;
import com.kapa.binance.service.external.OrderApi;
import com.kapa.binance.service.external.PositionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final RepeatSymbolRepository repeatSymbolRepository;
    private final OrderRepository orderRepository;
    private final StepRepository stepRepository;
    private final StepSymbolRepository stepSymbolRepository;
//    private final MaxVolSymbolRepository maxVolSymbolRepository;
//    private final MaxVolRepository maxVolRepository;
//    private final AccountApi accountApi;
    private final PositionApi positionApi;
    private final OrderApi orderApi;
    private final CopierService copierService;
    private final ObjectMapper objectMapper;
    private final LogLeverRepository logLeverRepository;

    @Override
    public void receiveMessage(AuthRequest authRequest, String message) {
        if (StringUtils.contains(message, "ACCOUNT_CONFIG_UPDATE")) {
            updateLever(authRequest.getUuid(), message);
            return;
        }

        if (!StringUtils.contains(message, "ORDER_TRADE_UPDATE")) return;
        log.info("Message: {}", message);
        DataOrder order = OrderMapper.mapDataOrder(message);
        if (order == null) {
            log.warn("receiveMessage - mapped order is null, skipping");
            return;
        }

        if (isFilledMarketOrLimit(order) || isExpiredTakeProfit(order)) {
            copierService.sendCopierOrder(order, authRequest);
            handleOrder(authRequest, order);
        } else {
            log.info("receiveMessage order not handled, conditions not met");
        }
    }

    private void updateLever(String uuid, String message) {
        try {
            JsonNode acNode = objectMapper.readTree(message).path("ac");
            String symbol = acNode.path("s").asText(null);
            int leverage = acNode.path("l").asInt(-1);

            if (symbol == null || leverage < 0) {
                log.warn("Invalid leverage update message: {}", message);
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            LogLeverEntity entity = logLeverRepository.findByUuidAndSymbol(uuid, symbol)
                    .orElseGet(() -> {
                        LogLeverEntity newEntity = new LogLeverEntity();
                        newEntity.setUuid(uuid);
                        newEntity.setSymbol(symbol);
                        newEntity.setCreatedAt(now);
                        return newEntity;
                    });

            entity.setLever(leverage);
            entity.setUpdatedAt(now);

            logLeverRepository.save(entity);

        } catch (Exception e) {
            log.error("Error updating leverage for uuid {}: {}", uuid, e.getMessage(), e);
        }
    }

    private boolean isFilledMarketOrLimit(DataOrder o) {
        return (OrderType.LIMIT.name().equals(o.getOrderType()) || OrderType.MARKET.name().equals(o.getOrderType()))
                && ExecutionTypeEnum.TRADE.name().equals(o.getExecutionType())
                && OrderStatusEnum.FILLED.name().equals(o.getOrderStatus());
    }

    private boolean isExpiredTakeProfit(DataOrder o) {
        return OrderType.TAKE_PROFIT_MARKET.name().equals(o.getOrderType())
                && ExecutionTypeEnum.EXPIRED.name().equals(o.getExecutionType())
                && OrderStatusEnum.EXPIRED.name().equals(o.getOrderStatus());
    }

    private void handleOrder(AuthRequest authRequest, DataOrder order) {
        String side = order.getSide(); // BUY or SELL
        log.info("handleOrder request, side={}, clientOrderId={}", side, order.getClientOrderId());
        if (SideEnum.BUY.name().equals(side)) {
            log.info("handleOrder - Processing BUY side");
            handlePosSideOrder(PositionSideEnum.SHORT, PositionSideEnum.LONG, authRequest, order);
        } else if (SideEnum.SELL.name().equals(side)) {
            log.info("handleOrder - Processing SELL side");
            handlePosSideOrder(PositionSideEnum.LONG, PositionSideEnum.SHORT, authRequest, order);
        } else {
            log.warn("handleOrder - Unsupported side: {}", side);
        }
    }

    private void handlePosSideOrder(PositionSideEnum closingSide, PositionSideEnum openingSide,
                                    AuthRequest authRequest, DataOrder order) {
        String positionSide = order.getPositionSide();
        String originOrderType = order.getOriginalOrderType();

        log.info("handlePosSideOrder request positionSide={}, originOrderType={}", positionSide, originOrderType);
        if (closingSide.name().equals(positionSide)) {
            log.info("handlePosSideOrder - Detected closing side: {}", positionSide);
            handleCloseOrder(authRequest, order); // Đóng position -> cancel các order con còn mở

            if (OrderType.TAKE_PROFIT_MARKET.name().equals(originOrderType)) {
                log.info("handlePosSideOrder - Handling take profit for order type: {}", originOrderType);
                handleTakeProfit(authRequest, order);
            }
        } else if (openingSide.name().equals(positionSide)) {
            log.info("handlePosSideOrder - Detected opening side: {}", positionSide);
            handleOpenOrder(authRequest, order);
        } else {
            log.warn("handlePosSideOrder - Unexpected position side: {}", positionSide);
        }
    }

    private void handleCloseOrder(AuthRequest authRequest, DataOrder order) {
        String symbol = order.getSymbol();
        String posSide = order.getPositionSide();

        log.info("handleCloseOrder request symbol={}, posSide={}", symbol, posSide);

        List<String> clientOrderIds = orderApi.getClientIdByOrderOpen(authRequest, symbol, posSide);
        log.info("handleCloseOrder - clientOrderIds to cancel: {}", clientOrderIds);

        orderApi.cancelOrderByClientIds(authRequest, symbol, clientOrderIds);
    }

    private void handleTakeProfit(AuthRequest authRequest, DataOrder order) {
        String symbol = order.getSymbol();
        String posSide = order.getPositionSide();

        log.info("handleTakeProfit - request symbol={}, posSide={}", symbol, posSide);
        RepeatSymbolEntity repeatEntity =
                repeatSymbolRepository.findFirstByUuidAndSymbolAndPosSideAndIsActiveTrue(authRequest.getUuid(), symbol, posSide
                );

        if (repeatEntity == null) {
            log.info("handleTakeProfit - Not found repeat entity for symbol={}, posSide={}", symbol, posSide);
            return;
        }

        Integer repeatCount = repeatEntity.getRepeatCount();
        if (repeatCount == null || repeatCount <= 0) {
            log.info("handleTakeProfit - RepeatCount = {}", repeatCount);
            return;
        }

        PositionInfo positionInfo = positionApi.getPosition(authRequest, symbol, posSide); // call api
        if (positionInfo != null && positionInfo.getPositionAmt() > 0) {
            log.info("handleTakeProfit - Take profit skipped because positionAmt > 0: {}", positionInfo.getPositionAmt());
            return;
        }

        log.info("handleTakeProfit - Creating market order with volume: {}", repeatEntity.getVolume());
        orderApi.createMarketOrder(authRequest, symbol, posSide, repeatEntity.getVolume()); // call api

        repeatEntity.setRepeatCount(repeatCount - 1);
        repeatSymbolRepository.save(repeatEntity);
    }

    private void handleOpenOrder(AuthRequest authRequest, DataOrder order) {
        String clientOrderId = order.getClientOrderId();
        String symbol = order.getSymbol();
        String positionSide = order.getPositionSide();

        log.info("handleOpenOrder request clientOrderId={}, symbol={}, positionSide={}", clientOrderId, symbol, positionSide);

        PositionInfo positionInfo = positionApi.getPosition(authRequest, symbol, positionSide);
        if (positionInfo == null) {
            log.warn("handleOpenOrder - No current position found for symbol={} side={}", symbol, positionSide);
            return;
        }

        boolean isExistingOrder =
                orderRepository.existsByUuidAndClientIdParentOrClientIdChildren(authRequest.getUuid(), clientOrderId, clientOrderId
                );
        if (isExistingOrder) {
            log.info("handleOpenOrder - Existing order found. Proceeding to update...");
            handleUpdateOrder(authRequest, order, positionInfo);
            return;
        }

        handleNewOrder(authRequest, order, positionInfo);
    }

    private void handleUpdateOrder(AuthRequest authRequest, DataOrder order, PositionInfo positionInfo) {
        String symbol = order.getSymbol();
        String clientOrderId = order.getClientOrderId();

        log.info("handleUpdateOrder request symbol={}, clientOrderId={}", symbol, clientOrderId);

        OrderEntity orderEntity = orderRepository.findFirstByUuidAndClientIdChildren(authRequest.getUuid(), clientOrderId);
        if (orderEntity == null) {
            log.error("handleUpdateOrder - No order found for symbol={} clientId={}", symbol, clientOrderId);
            return;
        }

        log.info("handleUpdateOrder - Closing open orders for clientId={}", clientOrderId);
        handleCloseOrder(authRequest, order); // Close open order

        int nextStep = orderEntity.getStep() + 1;
        log.info("handleUpdateOrder - Next step calculated: {}", nextStep);

        StepConfig config = findStepConfig(order, nextStep, authRequest.getUuid()); // Handle max step
        if (config == null) {
            log.info("handleUpdateOrder - No step config found for step={}, handling max step", nextStep);
            handleMaxStep(authRequest, order, orderEntity, positionInfo);
            return;
        }

        log.info("handleUpdateOrder - Creating or updating order with step={}", nextStep);
        createOrUpdateOrder(authRequest, order, config, positionInfo);
    }

    private void handleNewOrder(AuthRequest authRequest, DataOrder order, PositionInfo positionInfo) {
        String symbol = order.getSymbol();
        double orderQty = Math.abs(order.getOriginalQuantity());
        double positionAmt = Math.abs(positionInfo.getPositionAmt());

        log.info("handleNewOrder request symbol={}, orderQty={}, positionAmt={}", symbol, orderQty, positionAmt);

        if (Double.compare(orderQty, positionAmt) != 0) {
            log.info("handleNewOrder - Position changed by user. Skipping auto handle. symbol={}, qty={}, positionAmt={}",
                    symbol, orderQty, positionAmt);
            return;
        }

        StepConfig config = findStepConfig(order, 1, authRequest.getUuid());
        if (config == null) {
            log.warn("handleNewOrder - No step config found. Skipping auto handle. symbol={}", symbol);
            return;
        }

        log.info("handleNewOrder - Creating or updating order with step=1, symbol={}", symbol);
        createOrUpdateOrder(authRequest, order, config, positionInfo);
    }

    private StepConfig findStepConfig(DataOrder order, int step, String uuid) {
        StepSymbolEntity stepSymbol = stepSymbolRepository.findFirstByUuidAndSymbol(uuid, order.getSymbol());
        if (stepSymbol != null) {
            StepSymbolEntity config = stepSymbolRepository.findFirstByUuidAndSymbolAndStep(uuid, order.getSymbol(), step);
            return mapStep(config);
        } else {
            StepEntity config = stepRepository.findFirstByUuidAndStep(uuid, step);
            return mapStep(config);
        }
    }

    private StepConfig mapStep(Object step) {
        if (step == null) return null;

        StepConfig config = null;
        if (step instanceof StepSymbolEntity s) {
            config = StepConfig.builder()
                    .roi(s.getRoi())
                    .usdt(s.getUsdt())
                    .takeProfit(s.getTakeProfit())
                    .stopLoss(s.getStopLoss())
                    .lever(s.getLever())
                    .quantityDropPercent(s.getQuantityDropPercent())
                    .priceDropPercent(s.getPriceDropPercent())
                    .step(s.getStep())
                    .build();
        } else if (step instanceof StepEntity s) {
            config = StepConfig.builder()
                    .roi(s.getRoi())
                    .usdt(s.getUsdt())
                    .takeProfit(s.getTakeProfit())
                    .stopLoss(s.getStopLoss())
                    .lever(s.getLever())
                    .quantityDropPercent(s.getQuantityDropPercent())
                    .priceDropPercent(s.getPriceDropPercent())
                    .step(s.getStep())
                    .build();
        }

        log.info("Step config: {}", config);
        return config;
    }

    private void createOrUpdateOrder(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo) {
        String randomId = StringUtil.random(CommonConstant.DA);

//        if (!checkMaxVolume(authRequest, order.getSymbol(), order.getPositionSide(), config.getUsdt())) {
//            orderApi.createDca(authRequest, order, config, positionInfo, randomId);
//        }

        orderApi.createDca(authRequest, order, config, positionInfo, randomId);
        slAndTpAndDr(authRequest, order, config, positionInfo);
        saveOrder(authRequest, order, randomId, config.getStep(), config.getUsdt());
    }

    private void slAndTpAndDr(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo) {
        orderApi.createTP(authRequest, order, config, positionInfo);
        orderApi.createSL(authRequest, order, config, positionInfo);

//        if (config.getStep() != 1){
//            orderApi.createDR(authRequest, order, config, positionInfo);
//        }
    }

    private void saveOrder(AuthRequest authRequest, DataOrder order, String randomId, int step, Double vol) {
        String clientParentId = null;
        boolean isNew = true;
        if (step != 1) {
            isNew = false;
            clientParentId = order.getClientOrderId();
        }
        OrderEntity ordEntity = new OrderEntity();
        ordEntity.setClientIdParent(clientParentId);
        ordEntity.setClientIdChildren(randomId);
        ordEntity.setPosSide(order.getPositionSide());
        ordEntity.setSymbol(order.getSymbol());
        ordEntity.setStep(step);
        ordEntity.setMgnMode(order.getStpMode());
        ordEntity.setIsNew(isNew);
        ordEntity.setIsEndStep(false);
        ordEntity.setCreateAt(new Date());
        ordEntity.setUpdatedAt(new Date());
        ordEntity.setVolume(vol);
        ordEntity.setCTime(order.getOrderTradeTime());
        ordEntity.setUuid(authRequest.getUuid());

        orderRepository.save(ordEntity);
    }

    private void handleMaxStep(AuthRequest authRequest, DataOrder order, OrderEntity orderEntity, PositionInfo positionInfo) {
        String symbol = order.getSymbol();
        int currentStep = orderEntity.getStep();

        log.info("handleMaxStep request symbol={}, currentStep={}", symbol, currentStep);

        StepConfig config = findMaxStep(order, currentStep, authRequest.getUuid());
        if (config != null) {
            log.info("handleMaxStep - Found max step config. Creating take profit order. symbol={}, step={}", symbol, currentStep);
            slAndTpAndDr(authRequest, order, config, positionInfo);
        } else {
            log.info("handleMaxStep - Max step config not found or does not match for symbol={}, step={}", symbol, currentStep);
        }

        orderEntity.setIsEndStep(true);
        orderEntity.setUpdatedAt(new Date());
        orderRepository.save(orderEntity);

        log.info("handleMaxStep - Marked orderEntity as end step and saved. symbol={}, step={}", symbol, currentStep);
    }

    private StepConfig findMaxStep(DataOrder order, int step, String uuid) {
        StepSymbolEntity stepSymbol = stepSymbolRepository.findFirstByUuidAndSymbol(uuid, order.getSymbol());
        if (stepSymbol != null) {
            StepSymbolEntity config = stepSymbolRepository.findFirstByUuidAndSymbolAndStep(uuid, order.getSymbol(), step);
            if (config == null || config.getStep() != step) return null;
            return mapStep(config);
        } else {
            StepEntity config = stepRepository.findFirstByUuidAndStep(uuid, step);
            if (config == null || config.getStep() != step) return null;
            return mapStep(config);
        }
    }

    /*
    private boolean checkMaxVolume(AuthRequest auth, String symbol, String posSide, Double usdt) {
        Double balance = accountApi.getBalance(auth);
        if (ValidationUtil.isNulOrZero(balance)) return true;

        List<PositionInfo> positions = positionApi.getAllPosition(auth, symbol);
        if (positions.isEmpty()) return false;

        double totalLong = positions.stream()
                .filter(p -> PositionSideEnum.LONG.name().equalsIgnoreCase(p.getPositionSide()))
                .mapToDouble(PositionInfo::getNotional)
                .sum();

        double totalShort = positions.stream()
                .filter(p -> PositionSideEnum.SHORT.name().equalsIgnoreCase(p.getPositionSide()))
                .mapToDouble(PositionInfo::getNotional)
                .sum();

        double total = totalLong + totalShort;

        String uuid = auth.getUuid();
        return switch (posSide.toUpperCase()) {
            case "LONG" -> checkLimit(symbol, totalLong, total, usdt, balance, true, uuid);
            case "SHORT" -> checkLimit(symbol, totalShort, total, usdt, balance, false, uuid);
            default -> true;
        };
    }

    private boolean checkLimit(String symbol, double sideTotal, double total, double usdt, double bl, boolean isLong, String uuid) {
        log.info("Checking limit: symbol={}, sideTotal={}, total={}, usdt={}, bl={}, isLong={}, uuid={}",
                symbol, sideTotal, total, usdt, bl, isLong, uuid);

        MaxVolSymbolEntity insMax = isLong
                ? maxVolSymbolRepository.findFirstByUuidAndSymbolAndLongVolGreaterThanZero(uuid, symbol)
                : maxVolSymbolRepository.findFirstByUuidAndSymbolAndShortVolGreaterThanZero(uuid, symbol);

        if (insMax != null) {
            double maxVol = isLong ? insMax.getLongVol() : insMax.getShortVol();
            boolean result = sideTotal + usdt > bl * maxVol;
            log.info("Found insMax: maxVol={}, sideTotal + usdt = {}, threshold = {}, result={}",
                    maxVol, sideTotal + usdt, bl * maxVol, result);
            return result;
        }

        MaxVolEntity maxVol = maxVolRepository.findFirstByUuidOrderByIdDesc(uuid);
        if (maxVol == null) {
            log.warn("MaxVolEntity not found for uuid={}", uuid);
            return false;
        }

        Double sideVol = isLong ? maxVol.getLongVol() : maxVol.getShortVol();
        if (ValidationUtil.allNotNulNotZero(sideVol)) {
            boolean result = sideTotal + usdt > bl * sideVol;
            log.info("Using sideVol: sideVol={}, sideTotal + usdt = {}, threshold = {}, result={}",
                    sideVol, sideTotal + usdt, bl * sideVol, result);
            return result;
        }

        Double bothVol = maxVol.getBothVol();
        boolean result = ValidationUtil.allNotNulNotZero(bothVol) && total + usdt > bl * bothVol;
        log.info("Using bothVol: bothVol={}, total + usdt = {}, threshold = {}, result={}",
                bothVol, total + usdt, bl * bothVol, result);
        return result;
    }
    */
}
