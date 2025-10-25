package com.kapa.binance.service.external;

import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.PositionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionApi {

    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;

    public PositionInfo getPosition(AuthRequest authRequest, String symbol, String posSide) {
        return getAllPosition(authRequest, symbol).stream()
                .filter(p -> p.getPositionSide().equals(posSide))
                .findFirst().orElse(null);
    }

    public List<PositionInfo> getAllPosition(AuthRequest authRequest, String symbol) {
        try {
            RequestHandler requestHandler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());

            LinkedHashMap<String, Object> param = new LinkedHashMap<>();
            if (!StringUtils.isEmpty(symbol)) {
                param.put("symbol", symbol);
            }

            ResponseEntity<List<PositionInfo>> response = requestHandler.sendSignedRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v2/positionRisk",
                    param,
                    HttpMethod.GET,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .filter(position -> position.getPositionAmt() != null && position.getPositionAmt() != 0)
                        .toList();
            }
        } catch (Exception e) {
           log.error("Error fetching positions for symbol {}: {}", symbol, e.getMessage());
        }
        return new ArrayList<>();
    }


    public PositionInfo getPositionInfo(AuthRequest authRequest, String symbol, String positionSide) {
        try {
            RequestHandler requestHandler = new RequestHandler(
                    restTemplate,
                    authRequest.getApiKey(),
                    authRequest.getSecretKey()
            );

            LinkedHashMap<String, Object> param = new LinkedHashMap<>();
            if (!StringUtils.isEmpty(symbol)) {
                param.put("symbol", symbol.trim().toUpperCase());
            }

            ResponseEntity<List<PositionInfo>> response = requestHandler.sendSignedRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v2/positionRisk",
                    param,
                    HttpMethod.GET,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .filter(pos -> symbol == null || pos.getSymbol().equalsIgnoreCase(symbol))
                        .filter(pos -> positionSide == null || pos.getPositionSide().equalsIgnoreCase(positionSide))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Error fetching position for symbol {} side {}: {}", symbol, positionSide, e.getMessage(), e);
        }
        return null;
    }

}
