package com.kapa.binance.service.external;

import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.response.ListenKeyRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ListenKeyApi {

    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;

    public String create(String apiKey) {
        return requestListenKey(HttpMethod.POST, apiKey);
    }

    public boolean update(String apiKey) {
        return requestListenKey(HttpMethod.PUT, apiKey) != null;
    }

    private String requestListenKey(HttpMethod method, String apiKey) {
        try {
            log.info("Requesting listenKey with method: {}", method.name());

            RequestHandler requestHandler = new RequestHandler(restTemplate, apiKey);
            ResponseEntity<ListenKeyRes> response = requestHandler.sendWithApiKeyRequest(
                    envConfig.getBaseApi(),
                    "/fapi/v1/listenKey",
                    new LinkedHashMap<>(),
                    method,
                    ListenKeyRes.class
            );

            ListenKeyRes listenKeyRes = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && listenKeyRes != null) {
                log.info("Successfully {} listenKey: {}", method.name(), listenKeyRes.getListenKey());
                return listenKeyRes.getListenKey();
            }
            log.error("Failed to get listenKey: {}, status code: {}", listenKeyRes, response.getStatusCode());
            return null;
        } catch (HttpClientErrorException e){
            log.error("HTTP error while requesting listenKey: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while requesting listenKey: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
