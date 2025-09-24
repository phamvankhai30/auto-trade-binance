package com.kapa.binance.base.utils.http;

import com.kapa.binance.base.exception.Ex500;
import com.kapa.binance.base.utils.HmacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String secretKey;

    private enum RequestType {
        PUBLIC,
        WITH_API_KEY,
        SIGNED
    }

    @FunctionalInterface
    private interface ResponseExecutor<T> {
        ResponseEntity<T> execute(String url, HttpEntity<?> entity);
    }

    public RequestHandler(RestTemplate restTemplate) {
        this(restTemplate, null, null);
    }

    public RequestHandler(RestTemplate restTemplate, String apiKey) {
        this(restTemplate, apiKey, null);
    }

    public RequestHandler(RestTemplate restTemplate, String apiKey, String secretKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public <T> ResponseEntity<T> sendPublicRequest(String baseUrl, String urlPath,
                                                   LinkedHashMap<String, Object> parameters,
                                                   HttpMethod httpMethod,
                                                   Class<T> responseType) {
        return sendApiRequest(baseUrl, urlPath, null, parameters, httpMethod, RequestType.PUBLIC,
                (url, entity) -> restTemplate.exchange(URI.create(url), httpMethod, entity, responseType));
    }

    public <T> ResponseEntity<T> sendWithApiKeyRequest(String baseUrl, String urlPath,
                                                       LinkedHashMap<String, Object> parameters,
                                                       HttpMethod httpMethod,
                                                       Class<T> responseType) {
        validateApiKey();
        return sendApiRequest(baseUrl, urlPath, null, parameters, httpMethod, RequestType.WITH_API_KEY,
                (url, entity) -> restTemplate.exchange(URI.create(url), httpMethod, entity, responseType));
    }

    public <T> ResponseEntity<T> sendSignedRequest(String baseUrl, String urlPath,
                                                   LinkedHashMap<String, Object> parameters,
                                                   HttpMethod httpMethod,
                                                   Class<T> responseType) {
        validateCredentials();
        String signature = sign(baseUrl, parameters);
        return sendApiRequest(baseUrl, urlPath, signature, parameters, httpMethod, RequestType.SIGNED,
                (url, entity) -> restTemplate.exchange(URI.create(url), httpMethod, entity, responseType));
    }

    public <T> ResponseEntity<T> sendSignedRequest(String baseUrl, String urlPath,
                                                   LinkedHashMap<String, Object> parameters,
                                                   HttpMethod httpMethod,
                                                   ParameterizedTypeReference<T> responseType) {
        validateCredentials();
        String signature = sign(baseUrl, parameters);
        return sendApiRequest(baseUrl, urlPath, signature, parameters, httpMethod, RequestType.SIGNED,
                (url, entity) -> restTemplate.exchange(URI.create(url), httpMethod, entity, responseType));
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Ex500("[RequestHandler] API key cannot be null or empty!");
        }
    }

    private void validateCredentials() {
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new Ex500("[RequestHandler] API key or secret key cannot be null or empty!");
        }
    }

    private String sign(String baseUrl, LinkedHashMap<String, Object> parameters) {
        parameters.put("timestamp", fetchServerTime(baseUrl));
        String queryString = UrlBuilder.joinQueryParameters(parameters);
        return HmacUtil.getSignature(queryString, secretKey);
    }

    private HttpHeaders buildHeaders(RequestType requestType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "binance-futures-connector-java/3.0.4");
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        if (requestType == RequestType.WITH_API_KEY || requestType == RequestType.SIGNED) {
            headers.set("X-MBX-APIKEY", apiKey);
        }
        return headers;
    }

    private String buildUrlAndLog(String baseUrl, String urlPath, LinkedHashMap<String, Object> parameters,
                                  String signature, HttpMethod httpMethod) {
        String url = UrlBuilder.buildFullUrl(baseUrl, urlPath, parameters, signature);
        logger.info("[{}] {}", httpMethod, url);
        return url;
    }

    private <T> ResponseEntity<T> sendApiRequest(String baseUrl, String urlPath,
                                                 String signature,
                                                 LinkedHashMap<String, Object> parameters,
                                                 HttpMethod httpMethod,
                                                 RequestType requestType,
                                                 ResponseExecutor<T> executor) {
        try {
            String url = buildUrlAndLog(baseUrl, urlPath, parameters, signature, httpMethod);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(requestType));
            return executor.execute(url, entity);
        } catch (HttpClientErrorException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class BinanceTimeResponse {
        public long serverTime;
    }

    private long fetchServerTime(String baseUrl) {
        try {
            String timeUrl = baseUrl + "/fapi/v1/time";
            ResponseEntity<BinanceTimeResponse> response = restTemplate.getForEntity(timeUrl, BinanceTimeResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().serverTime;
            }
            return System.currentTimeMillis(); // Fallback to local time if API call fails
        } catch (Exception ex) {
            logger.error("Failed to fetch server time from Binance: {}", ex.getMessage(), ex);
            return System.currentTimeMillis(); // Fallback to local time if API call fails
        }
    }
}
