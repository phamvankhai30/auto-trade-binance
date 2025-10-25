package com.kapa.binance.service.external;

import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.FuturesBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountApi {
    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;

    public List<FuturesBalance> getFuturesBalance(AuthRequest authRequest) {
        try {
            log.info("Fetching futures balance for auth request");
            RequestHandler requestHandler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();

            ResponseEntity<List<FuturesBalance>> response = requestHandler.sendSignedRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v3/balance",
                    params,
                    HttpMethod.GET,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Fetched account info successfully {}", response.getBody());
                return response.getBody() == null ? List.of() : response.getBody();
            }
            log.error("Failed to fetch account info: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Error fetching account info: {}", e.getMessage());
        }
        return List.of();
    }

    public Double getBalance(AuthRequest authRequest) {
        try {
            List<FuturesBalance> balances = getFuturesBalance(authRequest);
            if (balances.isEmpty()) return 0.0;

            return balances.stream()
                    .filter(balance -> "USDT".equals(balance.getAsset()))
                    .map(FuturesBalance::getBalance)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            log.error("Error fetching balance: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    public void changeLeverage(AuthRequest authRequest, String symbol, Integer leverage) {
        try {
            log.info("Changing leverage for symbol: {} to {}", symbol, leverage);
            RequestHandler requestHandler = new RequestHandler(restTemplate, authRequest.getApiKey(), authRequest.getSecretKey());
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("leverage", leverage);

            ResponseEntity<LinkedHashMap<String, Object>> response = requestHandler.sendSignedRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v1/leverage",
                    params,
                    HttpMethod.POST,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Leverage changed successfully to");
            }
            log.error("Failed to change leverage: {} {}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Error changing leverage: {}", e.getMessage());
        }
    }
}
