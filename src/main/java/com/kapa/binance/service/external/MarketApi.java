package com.kapa.binance.service.external;

import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.response.MarketInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketApi {

    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;

    public double getPriceBySymbol(String symbol) {
        try {
            RequestHandler requestHandler = new RequestHandler(restTemplate);
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);

            ResponseEntity<MarketInfo> response = requestHandler.sendPublicRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v1/premiumIndex",
                    params,
                    HttpMethod.GET,
                    MarketInfo.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getMarkPrice();
            }

            log.warn("Failed to fetch price for symbol {}: {} {}", symbol, response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Error fetching price for symbol {}: {}", symbol, e.getMessage(), e);
        }

        return 0.0;
    }
}
