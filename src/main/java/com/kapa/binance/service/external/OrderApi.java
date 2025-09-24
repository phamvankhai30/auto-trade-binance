package com.kapa.binance.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapa.binance.base.response.OrderInfo;
import com.kapa.binance.base.utils.CalculatorUtil;
import com.kapa.binance.base.utils.StringUtil;
import com.kapa.binance.base.utils.ValidationUtil;
import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.constant.CommonConstant;
import com.kapa.binance.enums.OrderType;
import com.kapa.binance.enums.PositionSideEnum;
import com.kapa.binance.enums.SideEnum;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.dtos.StepConfig;
import com.kapa.binance.model.response.DataOrder;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.model.response.SymbolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApi {

    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SymbolApi symbolApi;
    private final MarketApi marketApi;

    public List<String> getClientIdByOrderOpen(AuthRequest authRequest, String symbol, String positionSide) {
        log.info("getClientIdByOrderOpen request symbol={}, positionSide={}", symbol, positionSide);

        List<String> clientIds = getAllOpenOrders(authRequest, symbol).stream()
                .filter(order -> positionSide.equals(order.getPositionSide()))
                .map(OrderInfo::getClientOrderId)
                .collect(Collectors.toList());

        log.info("getClientIdByOrderOpen response clientIds={}", clientIds);
        return clientIds;
    }

    public List<OrderInfo> getAllOpenOrders(AuthRequest authRequest, String symbol) {
        try {
            log.info("getAllOpenOrders request symbol={}", symbol);

            RequestHandler requestHandler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());

            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            if (StringUtils.isNotEmpty(symbol)) {
                parameters.put("symbol", symbol);
            }

            ResponseEntity<List<OrderInfo>> response = requestHandler.sendSignedRequest(
                    envConfig.getBaseApi(), "/fapi/v1/allOrders", parameters, HttpMethod.GET,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                List<OrderInfo> orders = Optional.ofNullable(response.getBody())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(order -> "NEW".equals(order.getStatus()))
                        .collect(Collectors.toList());

                log.info("getAllOpenOrders response returned {} new orders", orders.size());
                return orders;
            } else {
                log.warn("getAllOpenOrders non-success status={}, body={}",  response.getStatusCode(), response.getBody());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("getAllOpenOrders error {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void cancelOrderByClientIds(AuthRequest authRequest, String symbol, List<String> clientOrderIds) {
        try {
            log.info("cancelOrderByClientIds request symbol={}, clientIds={}", symbol, clientOrderIds);

            if (StringUtils.isEmpty(symbol) || !ValidationUtil.hasValue(clientOrderIds)) {
                log.info("Cancel order skipped: missing symbol or empty clientOrderIds");
                return;
            }

            if (clientOrderIds.size() > 10) {
                clientOrderIds = clientOrderIds.subList(0, 10);
            }

            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("origClientOrderIdList", objectMapper.writeValueAsString(clientOrderIds));

            RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
            ResponseEntity<String> response = handler.sendSignedRequest(
                    envConfig.getBaseApi(), "/fapi/v1/batchOrders", params, HttpMethod.DELETE, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Cancelled orders successfully, clientOrderIds={}", clientOrderIds);
            } else {
                log.warn("Cancel failed for clientOrderIds={}, response={}", clientOrderIds, response.getBody());
            }

        } catch (Exception e) {
            log.error("cancelOrderByClientIds error={}", e.getMessage());
        }
    }

    public void createMarketOrder(AuthRequest authRequest, String symbol, String posSide, Double volume) {
        try {
            SymbolInfo symbolInfo = getSymbolInfoOrLog(symbol);
            if (symbolInfo == null || volume == 0) {
                log.warn("createMarketOrder skipped: symbolInfo is null or volume is zero for symbol '{}'", symbol);
                return;
            }

            double marketPrice = marketApi.getPriceBySymbol(symbol);
            if (marketPrice == 0) {
                log.warn("Market price is zero for symbol '{}'", symbol);
                return;
            }

            BigDecimal quantity = CalculatorUtil.quantity(volume, marketPrice, symbolInfo.getStepSize(), symbolInfo.getQuantityPrecision());
            if (quantity.doubleValue() == 0) {
                log.warn("Quantity is zero for symbol '{}', volume={}, price={}", symbol, volume, marketPrice);
                return;
            }

            sendMarketOrder(authRequest, symbol, posSide, getSideFromMarket(posSide), quantity);
        } catch (Exception e) {
            log.error("Failed to create market order for '{}': {}", symbol, e.getMessage());
        }
    }

    public void createDca(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo, String clientId) {
        try {
            if (!isValidDCAInput(order, config, positionInfo)) {
                log.warn("createDCA skipped: invalid input parameters for order={}, config={}, positionInfo={}", order, config, positionInfo);
            }

            String symbol = order.getSymbol();
            String posSide = order.getPositionSide();
            SymbolInfo symbolInfo = getSymbolInfoOrLog(symbol);
            if (symbolInfo == null) return;

            BigDecimal dcaPrice = calculateDCAPrice(positionInfo.getEntryPrice(), config.getRoi(), config.getLever(), symbolInfo, posSide);
            if (dcaPrice == null || dcaPrice.doubleValue() == 0) {
                log.warn("DCA price is null or zero for symbol '{}', entryPrice={}, roi={}, lever={}", symbol, positionInfo.getEntryPrice(), config.getRoi(), config.getLever());
                return;
            }

            BigDecimal quantity = CalculatorUtil.quantity(config.getUsdt(), dcaPrice.doubleValue(), symbolInfo.getStepSize(), symbolInfo.getQuantityPrecision());
            if (quantity.doubleValue() == 0) {
                log.warn("Calculated quantity is zero for symbol '{}', usdt={}, price={}", symbol, config.getUsdt(), dcaPrice);
                return;
            }

            sendLimitOrder(authRequest, symbol, posSide, getSideFromDca(posSide), dcaPrice, quantity, clientId);
        } catch (Exception e) {
            log.error("Error creating DCA order: {}", e.getMessage());
        }
    }

    public void createTP(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo) {
        try {
            if (!isValidTPInput(order, config, positionInfo)) {
                log.warn("createTP skipped: invalid input parameters for order={}, config={}, positionInfo={}", order, config, positionInfo);
                return;
            }

            String symbol = order.getSymbol();
            String posSide = order.getPositionSide();
            SymbolInfo symbolInfo = getSymbolInfoOrLog(symbol);
            if (symbolInfo == null) return;

            BigDecimal tpPrice = calculateTPPrice(positionInfo.getEntryPrice(), config.getTakeProfit(), config.getLever(), symbolInfo, posSide);
            if (tpPrice == null || tpPrice.doubleValue() == 0) {
                log.warn("TP price is null or zero for symbol '{}', entryPrice={}, takeProfit={}, lever={}", symbol, positionInfo.getEntryPrice(), config.getTakeProfit(), config.getLever());
                return;
            }

            sendTPOrder(authRequest, symbol, posSide, getSideFromTP(posSide), tpPrice);
        } catch (Exception e) {
            log.error("Error creating TP order: {}", e.getMessage());
        }
    }

    public void createSL(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo) {
        try {
            if (!isValidSLInput(order, config, positionInfo)) {
                log.warn("Skip SL: invalid input parameters for order={}, config={}, positionInfo={}", order, config, positionInfo);
                return;
            }

            String symbol = order.getSymbol();
            String posSide = order.getPositionSide();
            SymbolInfo symbolInfo = getSymbolInfoOrLog(symbol);
            if (symbolInfo == null) return;

            BigDecimal slPrice = calculateSLPrice(positionInfo.getEntryPrice(), config.getStopLoss(), config.getLever(), symbolInfo, posSide);
            if (slPrice == null || slPrice.doubleValue() == 0) {
                log.warn("SL price is null or zero for symbol={}, entryPrice={}, sl={}, lever={}", symbol, positionInfo.getEntryPrice(), config.getStopLoss(), config.getLever());
                return;
            }

            sendSLOrder(authRequest, symbol, posSide, getSideFromSL(posSide), slPrice);
        } catch (Exception e) {
            log.error("Error creating SL order: {}", e.getMessage());
        }
    }

    public void createDR(AuthRequest authRequest, DataOrder order, StepConfig config, PositionInfo positionInfo) {
        try {
            if (!isValidDRInput(order, config, positionInfo)) {
                log.warn("createDR skipped: invalid input parameters for order={}, config={}, positionInfo={}", order, config, positionInfo);
                return;
            }

            String symbol = order.getSymbol();
            String posSide = order.getPositionSide();
            SymbolInfo symbolInfo = getSymbolInfoOrLog(symbol);
            if (symbolInfo == null) return;

            BigDecimal drPrice = calculateDRPrice(positionInfo.getEntryPrice(), config.getPriceDropPercent(), config.getLever(), symbolInfo, posSide);
            double quantityDrop = CalculatorUtil.reduceQuantity(positionInfo.getPositionAmt(), config.getQuantityDropPercent(), symbolInfo.getStepSize(), symbolInfo.getQuantityPrecision());

            if (drPrice == null || ValidationUtil.isNulOrZero(quantityDrop)) {
                log.info("DR price or quantity is null or zero for symbol '{}', entryPrice={}, priceDropPercent={}, lever={}",
                        symbol, positionInfo.getEntryPrice(), config.getPriceDropPercent(), config.getLever());
                return;
            }

            String side = getSideFromDR(posSide);
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("positionSide", posSide);
            params.put("side", side);
            params.put("quantity", quantityDrop);
            params.put("timeInForce", "GTC");
            params.put("newClientOrderId", StringUtil.random(CommonConstant.DR));

            if (PositionSideEnum.LONG.name().equals(posSide)) {
                if (drPrice.doubleValue() < positionInfo.getEntryPrice()) {
                    params.put("type", OrderType.STOP_MARKET.name()); //sl
                    params.put("stopPrice", drPrice);
                } else {
                    params.put("type", OrderType.LIMIT.name()); //tp
                    params.put("price", drPrice);
                }
            } else {
                if (drPrice.doubleValue() > positionInfo.getEntryPrice()) {
                    params.put("type", OrderType.STOP_MARKET.name());
                    params.put("stopPrice", drPrice);
                } else {
                    params.put("type", OrderType.LIMIT.name());
                    params.put("price", drPrice);
                }
            }

            RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
            ResponseEntity<String> response = handler.sendSignedRequest(envConfig.getBaseApi(), "/fapi/v1/order", params, HttpMethod.POST, String.class);
            log.info("DR order placed: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error creating DR order: {}", e.getMessage());
        }
    }

    private void sendMarketOrder(AuthRequest authRequest, String symbol, String posSide, String side, BigDecimal quantity) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("positionSide", posSide);
        params.put("side", side);
        params.put("type", OrderType.MARKET.name());
        params.put("quantity", quantity);

        RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
        ResponseEntity<String> response = handler.sendSignedRequest(envConfig.getBaseApi(), "/fapi/v1/order", params, HttpMethod.POST, String.class);
        log.info("Market order created: {}", response.getBody());
    }

    private void sendLimitOrder(AuthRequest authRequest, String symbol, String posSide, String side,
                                BigDecimal price, BigDecimal quantity, String clientId) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("positionSide", posSide);
        params.put("side", side);
        params.put("type", OrderType.LIMIT.name());
        params.put("quantity", quantity);
        params.put("price", price);
        params.put("timeInForce", "GTC");
        params.put("newClientOrderId", clientId);

        RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
        ResponseEntity<String> response = handler.sendSignedRequest(envConfig.getBaseApi(), "/fapi/v1/order", params, HttpMethod.POST, String.class);
        log.info("Send DCA: {}", response.getBody());
    }

    private void sendTPOrder(AuthRequest authRequest, String symbol, String posSide, String side, BigDecimal stopPrice) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("positionSide", posSide);
        params.put("side", side);
        params.put("type", OrderType.TAKE_PROFIT_MARKET.name());
        params.put("stopPrice", stopPrice);
        params.put("closePosition", true);
        params.put("newClientOrderId", StringUtil.random(CommonConstant.TP));

        RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
        ResponseEntity<String> response = handler.sendSignedRequest(envConfig.getBaseApi(), "/fapi/v1/order", params, HttpMethod.POST, String.class);
        log.info("Send TP: {}", response.getBody());
    }

    private void sendSLOrder(AuthRequest authRequest, String symbol, String posSide, String side, BigDecimal stopPrice) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("positionSide", posSide);
        params.put("side", side);
        params.put("type", OrderType.STOP_MARKET.name());
        params.put("stopPrice", stopPrice);
        params.put("closePosition", true);
        params.put("newClientOrderId", StringUtil.random(CommonConstant.SL));

        RequestHandler handler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
        ResponseEntity<String> response = handler.sendSignedRequest(envConfig.getBaseApi(), "/fapi/v1/order", params, HttpMethod.POST, String.class);
        log.info("SL order placed: {}", response.getBody());
    }

    private SymbolInfo getSymbolInfoOrLog(String symbol) {
        SymbolInfo symbolInfo = symbolApi.getSymbolInfo(symbol);
        if (symbolInfo == null) log.error("Symbol info not found for '{}'", symbol);
        return symbolInfo;
    }

    private String getSideFromMarket(String posSide) {
        return PositionSideEnum.LONG.name().equals(posSide) ? SideEnum.BUY.name() : SideEnum.SELL.name();
    }

    private String getSideFromDca(String posSide) {
        return PositionSideEnum.LONG.name().equals(posSide) ? SideEnum.BUY.name() : SideEnum.SELL.name();
    }

    private String getSideFromTP(String posSide) {
        return PositionSideEnum.LONG.name().equals(posSide) ? SideEnum.SELL.name() : SideEnum.BUY.name();
    }

    private String getSideFromSL(String posSide) {
        return PositionSideEnum.LONG.name().equals(posSide) ? SideEnum.SELL.name() : SideEnum.BUY.name();
    }

    private String getSideFromDR(String posSide) {
        return PositionSideEnum.LONG.name().equals(posSide) ? SideEnum.SELL.name() : SideEnum.BUY.name();
    }

    private BigDecimal calculateDCAPrice(double entryPrice, double roi, int leverage, SymbolInfo info, String posSide) {
        return switch (PositionSideEnum.valueOf(posSide)) {
            case LONG ->
                    CalculatorUtil.dcaLongPrice(entryPrice, roi, leverage, info.getTickSize(), info.getPricePrecision());
            case SHORT ->
                    CalculatorUtil.dcaShortPrice(entryPrice, roi, leverage, info.getTickSize(), info.getPricePrecision());
            default -> null;
        };
    }

    private BigDecimal calculateDRPrice(double entryPrice, double pricePercent, int leverage, SymbolInfo info, String posSide) {
        return switch (PositionSideEnum.valueOf(posSide)) {
            case LONG ->
                    CalculatorUtil.priceDrop(entryPrice, pricePercent, leverage, info.getTickSize(), info.getPricePrecision(), true);
            case SHORT ->
                    CalculatorUtil.priceDrop(entryPrice, pricePercent, leverage, info.getTickSize(), info.getPricePrecision(), false);
            default -> null;
        };
    }

    private BigDecimal calculateTPPrice(double entryPrice, double tp, int leverage, SymbolInfo info, String posSide) {
        return switch (PositionSideEnum.valueOf(posSide)) {
            case LONG ->
                    CalculatorUtil.takeProfitLongPrice(entryPrice, tp, leverage, info.getTickSize(), info.getPricePrecision());
            case SHORT ->
                    CalculatorUtil.takeProfitShortPrice(entryPrice, tp, leverage, info.getTickSize(), info.getPricePrecision());
            default -> null;
        };
    }

    private BigDecimal calculateSLPrice(double entryPrice, double sl, int leverage, SymbolInfo info, String posSide) {
        return switch (PositionSideEnum.valueOf(posSide)) {
            case LONG ->
                    CalculatorUtil.dcaLongPrice(entryPrice, sl, leverage, info.getTickSize(), info.getPricePrecision());
            case SHORT ->
                    CalculatorUtil.dcaShortPrice(entryPrice, sl, leverage, info.getTickSize(), info.getPricePrecision());
            default -> null;
        };
    }

    private boolean isValidDCAInput(DataOrder order, StepConfig config, PositionInfo info) {
        return order != null && config != null && info != null &&
                StringUtils.isNoneEmpty(order.getSymbol(), order.getPositionSide()) &&
                ValidationUtil.allNotNulNotZero(config.getRoi(), config.getUsdt(), config.getLever(), info.getEntryPrice());
    }

    private boolean isValidTPInput(DataOrder order, StepConfig config, PositionInfo info) {
        return order != null && config != null && info != null &&
                StringUtils.isNoneEmpty(order.getSymbol(), order.getPositionSide()) &&
                ValidationUtil.allNotNulNotZero(config.getTakeProfit(), config.getLever(), info.getEntryPrice());
    }

    private boolean isValidSLInput(DataOrder order, StepConfig config, PositionInfo info) {
        return order != null && config != null && info != null &&
                StringUtils.isNoneEmpty(order.getSymbol(), order.getPositionSide()) &&
                ValidationUtil.allNotNulNotZero(config.getStopLoss(), config.getLever(), info.getEntryPrice());
    }

    private boolean isValidDRInput(DataOrder order, StepConfig config, PositionInfo info) {
        return order != null && config != null && info != null &&
                StringUtils.isNoneEmpty(order.getSymbol(), order.getPositionSide()) &&
                ValidationUtil.allNotNulNotZero(config.getQuantityDropPercent(), config.getPriceDropPercent(),
                        config.getLever(), info.getEntryPrice(), info.getPositionAmt());
    }
}
