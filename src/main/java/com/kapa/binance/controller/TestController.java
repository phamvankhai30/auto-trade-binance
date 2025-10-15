//package com.kapa.binance.controller;
//
//import com.kapa.binance.base.utils.StringUtil;
//import com.kapa.binance.model.dtos.AuthRequest;
//import com.kapa.binance.model.dtos.StepConfig;
//import com.kapa.binance.model.response.DataOrder;
//import com.kapa.binance.model.response.PositionInfo;
//import com.kapa.binance.service.UserService;
//import com.kapa.binance.service.external.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@RequiredArgsConstructor
//@RestController
//@RequestMapping("/test")
//public class TestController {
//
//    private final MarketApi marketApi;
//    private final SymbolApi symbolApi;
//    private final PositionApi positionApi;
//    private final OrderApi orderApi;
//    private final UserService userService;
//    private final AccountApi accountApi;
//
//    private static AuthRequest getAuthRequest() {
//        AuthRequest authRequest = new AuthRequest();
//        authRequest.setUuid("123456789");
//        authRequest.setApiKey("ae144faa872b0e022c00a3160989dcc7545b5c140260d5148731a8f7c79018e8");
//        authRequest.setSecretKey("8234dd5e0623f8ba49813b08d2aa4d9ed3d8aa7dd58e4fb8861bfc22cbeb74a6");
//        return authRequest;
//    }
//
//    @GetMapping("/market-price")
//    public ResponseEntity<?> markPrice(@RequestParam String symbol) {
//        return ResponseEntity.ok(marketApi.getPriceBySymbol(symbol));
//    }
//
//    @GetMapping("/symbol-info")
//    public ResponseEntity<?> symbolInfo(@RequestParam String symbol) {
//        return ResponseEntity.ok(symbolApi.getSymbolInfo(symbol));
//    }
//
//    @GetMapping("/position-info")
//    public ResponseEntity<?> position(@RequestParam String symbol, @RequestParam String positionSide) {
//        return ResponseEntity.ok(positionApi.getPosition(getAuthRequest(), symbol, positionSide));
//    }
//
//    @GetMapping("/open-info")
//    public ResponseEntity<?> open(@RequestParam String symbol, @RequestParam String positionSide) {
//        List<String> ls = orderApi.getClientIdByOrderOpen(getAuthRequest(), symbol, positionSide);
//        return ResponseEntity.ok(ls);
//    }
//
//    @GetMapping("/cancel-open")
//    public ResponseEntity<?> cancel(@RequestParam String symbol, @RequestParam String positionSide) {
//        AuthRequest authRequest = getAuthRequest();
//        List<String> ls = orderApi.getClientIdByOrderOpen(authRequest, symbol, positionSide);
//        orderApi.cancelOrderByClientIds(authRequest, symbol, ls);
//        return ResponseEntity.ok(ls);
//    }
//
//    @GetMapping("/create-market-order")
//    public ResponseEntity<?> createMarketOrder(@RequestParam String symbol,
//                                               @RequestParam String positionSide,
//                                               @RequestParam double volume) {
//        orderApi.createMarketOrder(getAuthRequest(), symbol, positionSide, volume);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/create-limit-order")
//    public ResponseEntity<?> createLimitOrder(@RequestParam String symbol,
//                                              @RequestParam String positionSide,
//                                              @RequestParam double roi,
//                                              @RequestParam double usdt,
//                                              @RequestParam int leverage,
//                                              @RequestParam double entryPrice) {
//        DataOrder order = new DataOrder();
//        order.setSymbol(symbol);
//        order.setPositionSide(positionSide);
//
//        StepConfig config = new StepConfig();
//        config.setRoi(roi);
//        config.setUsdt(usdt);
//        config.setLever(leverage);
//
//        PositionInfo positionInfo = new PositionInfo();
//        positionInfo.setEntryPrice(entryPrice);
//
//        orderApi.createDca(getAuthRequest(), order, config, positionInfo, StringUtil.random());
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/create-take-profit")
//    public ResponseEntity<?> createTP(@RequestParam String symbol,
//                                      @RequestParam String positionSide,
//                                      @RequestParam double tpPercent,
//                                      @RequestParam int leverage,
//                                      @RequestParam double entryPrice) {
//        DataOrder order = new DataOrder();
//        order.setSymbol(symbol);
//        order.setPositionSide(positionSide);
//
//        StepConfig config = new StepConfig();
//        config.setLever(leverage);
//        config.setTakeProfit(tpPercent);
//
//        PositionInfo positionInfo = new PositionInfo();
//        positionInfo.setEntryPrice(entryPrice);
//
//        orderApi.createTP(getAuthRequest(), order, config, positionInfo);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/balance")
//    public ResponseEntity<?> balance() {
//        return ResponseEntity.ok(accountApi.getBalance(getAuthRequest()));
//    }
//
//    @GetMapping("/stop-loss")
//    public ResponseEntity<?> stopLoss(@RequestParam String symbol,
//                                      @RequestParam String positionSide,
//                                      @RequestParam double stopLoss,
//                                      @RequestParam int leverage,
//                                      @RequestParam double entryPrice) {
//        DataOrder order = new DataOrder();
//        order.setSymbol(symbol);
//        order.setPositionSide(positionSide);
//
//        StepConfig config = new StepConfig();
//        config.setLever(leverage);
//        config.setStopLoss(stopLoss);
//
//        PositionInfo positionInfo = new PositionInfo();
//        positionInfo.setEntryPrice(entryPrice);
//        orderApi.createSL(getAuthRequest(), order, config, positionInfo);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/drop-volume")
//    public ResponseEntity<?> dropVolume(@RequestParam String symbol,
//                                        @RequestParam String positionSide,
//                                        @RequestParam double quantityDropPercent,
//                                        @RequestParam double priceDropPercent,
//                                        @RequestParam int leverage,
//                                        @RequestParam double amt,
//                                        @RequestParam double entryPrice) {
//        DataOrder order = new DataOrder();
//        order.setSymbol(symbol);
//        order.setPositionSide(positionSide);
//
//        StepConfig config = new StepConfig();
//        config.setLever(leverage);
//        config.setQuantityDropPercent(quantityDropPercent);
//        config.setPriceDropPercent(priceDropPercent);
//
//        PositionInfo positionInfo = new PositionInfo();
//        positionInfo.setEntryPrice(entryPrice);
//        positionInfo.setPositionAmt(amt);
//        orderApi.createDR(getAuthRequest(), order, config, positionInfo);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/close-full-position")
//    public ResponseEntity<?> closeFullPosition() {
//        try {
//            orderApi.closePosition(getAuthRequest(), "XRPUSDT", "LONG", BigDecimal.valueOf(69.9));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ResponseEntity.ok().build();
//    }
//}