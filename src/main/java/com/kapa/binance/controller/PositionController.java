package com.kapa.binance.controller;

import com.kapa.binance.base.utils.CalculatorUtil;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.request.ClosePositionReq;
import com.kapa.binance.model.response.ClosePositionUserRes;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.model.response.SymbolInfo;
import com.kapa.binance.service.CopierAccountService;
import com.kapa.binance.service.external.OrderApi;
import com.kapa.binance.service.external.PositionApi;
import com.kapa.binance.service.external.SymbolApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

    private final CopierAccountService copierAccountService;
    private final PositionApi positionApi;
    private final SymbolApi symbolApi;
    private final OrderApi orderApi;

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/close")
    public ResponseEntity<?> closePositions(@RequestBody @Valid ClosePositionReq req) {
        log.info("Close Position Request: {}", req);

        String uuid = req.getUuid();
        String symbol = req.getSymbol().trim().toUpperCase();
        String positionSide = req.getPositionSide().trim().toUpperCase();

        List<ClosePositionUserRes> responseList = new ArrayList<>();

        if (req.getIsCopier()) {
            // Handle copier request
            AuthRequest copierAuth = copierAccountService.getCopierAuthByCopier(uuid);
            ClosePositionUserRes result = processCloseRequest(copierAuth, symbol, positionSide, req.getClosePercent());
            responseList.add(result);
        } else {
            // Handle leader request for multiple copiers
            List<AuthRequest> leaderAuths = copierAccountService.getCopierAuthByLeader(uuid);
            for (AuthRequest auth : leaderAuths) {
                ClosePositionUserRes result = processCloseRequest(auth, symbol, positionSide, req.getClosePercent());
                if (result != null) {
                    responseList.add(result);
                }
            }
        }

        return ResponseEntity.ok(responseList);
    }

    /**
     * Processes a single close position request for a given authenticated user.
     */
    private ClosePositionUserRes processCloseRequest(AuthRequest auth, String symbol, String positionSide, double closePercent) {
        PositionInfo positionInfo = positionApi.getPosition(auth, symbol, positionSide);
        if (positionInfo == null || positionInfo.getPositionAmt() == 0) {
            log.warn("No open position for {} [{} - {}]", auth.getFullName(), symbol, positionSide);
            return null;
        }

        double totalAmount = Math.abs(positionInfo.getPositionAmt());
        double amountToClose = totalAmount * (closePercent / 100.0);

        SymbolInfo symbolInfo = symbolApi.getSymbolInfo(symbol);
        BigDecimal quantity = CalculatorUtil.roundValue(
                amountToClose,
                symbolInfo.getStepSize(),
                symbolInfo.getQuantityPrecision()
        );

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Calculated quantity <= 0 for {} [{} - {}]", auth.getFullName(), symbol, positionSide);
            return null;
        }

        String side = positionSide.equalsIgnoreCase("LONG") ? "SELL" : "BUY";
        ClosePositionUserRes response = new ClosePositionUserRes();

        response.setSymbol(symbol);
        response.setPositionSide(positionSide);
        response.setClosedQuantity(quantity.doubleValue());
        response.setUuid(auth.getUuid());
        response.setFullName(auth.getFullName());

        try {
            orderApi.closePosition(auth, symbol, positionSide, side, quantity);
        } catch (HttpClientErrorException e) {
            response.setErrorMessage(e.getResponseBodyAsString());
            log.error("Failed to close position for {} [{} - {}]: {}", auth.getFullName(), symbol, positionSide, e.getMessage());
        }

        return response;
    }

//    @PostMapping("/open-positions")
//    public ResponseEntity<?> openPositions(@RequestBody @Valid OpenPositionReq req) {
//        log.info("Open Position Request: {}", req);
//
//        String uuid = req.getUuid();
//        String symbol = req.getSymbol().trim().toUpperCase();
//        String positionSide = req.getPositionSide().trim().toUpperCase();
//        Double usdt = req.getUsdt();
//        Integer leverage = req.getLeverage();
//
//        List<OpenPositionUserRes> responseList = new ArrayList<>();
//
//        AuthRequest copierAuth = copierAccountService.getCopierAuthByCopier(uuid);
//
//        return ResponseEntity.ok(responseList);
//    }
}
