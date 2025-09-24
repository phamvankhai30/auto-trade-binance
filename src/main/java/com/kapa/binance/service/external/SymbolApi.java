package com.kapa.binance.service.external;

import com.kapa.binance.base.utils.http.RequestHandler;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.response.ExchangeInfo;
import com.kapa.binance.model.response.Filter;
import com.kapa.binance.model.response.RawSymbolInfo;
import com.kapa.binance.model.response.SymbolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymbolApi {

    private final EnvConfig envConfig;
    private final RestTemplate restTemplate;
    private volatile ExchangeInfo cachedExchangeInfo;
    private volatile long cacheTimestamp = 0L;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L; // 30 phút

    public SymbolInfo getSymbolInfo(String symbol) {
        SymbolInfo symbolInfo = findSymbolInCache(symbol);

        if (symbolInfo != null) {
            return symbolInfo;
        }

        synchronized (this) {
            cachedExchangeInfo = fetchExchangeInfo();
            cacheTimestamp = Instant.now().toEpochMilli();
        }

        return findSymbolInCache(symbol);
    }

    private SymbolInfo findSymbolInCache(String symbol) {
        ExchangeInfo exchangeInfo = getExchangeInfoCached();

        return Optional.ofNullable(exchangeInfo)
                .map(ExchangeInfo::getSymbols)
                .flatMap(symbols -> symbols.stream()
                        .filter(s -> s.getSymbol().equalsIgnoreCase(symbol))
                        .findFirst()
                )
                .map(this::toSymbolInfo)
                .orElse(null);
    }

    public ExchangeInfo getExchangeInfoCached() {
        long now = Instant.now().toEpochMilli();

        if (cachedExchangeInfo == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            synchronized (this) {
                if (cachedExchangeInfo == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
                    log.info("Cache expired or empty, fetching new exchange info...");
                    cachedExchangeInfo = fetchExchangeInfo();
                    cacheTimestamp = now;
                }
            }
        }
        return cachedExchangeInfo;
    }

    private ExchangeInfo fetchExchangeInfo() {
        try {
            RequestHandler requestHandler = new RequestHandler(restTemplate);
            ResponseEntity<ExchangeInfo> response = requestHandler.sendPublicRequest(envConfig.getBaseApi(),
                    "/fapi/v1/exchangeInfo",
                    new LinkedHashMap<>(),
                    HttpMethod.GET,
                    ExchangeInfo.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Failed to fetch exchange info: {} {}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Error fetching exchange info: {}", e.getMessage(), e);
        }
        return null;
    }

    private SymbolInfo toSymbolInfo(RawSymbolInfo raw) {
        SymbolInfo info = new SymbolInfo();
        info.setSymbol(raw.getSymbol());
        info.setQuantityPrecision(raw.getQuantityPrecision());
        info.setPricePrecision(raw.getPricePrecision());

        for (Filter filter : raw.getFilters()) {
            switch (filter.getFilterType()) {
                case "PRICE_FILTER" -> {
                    info.setTickSize(filter.getTickSize());
                    info.setMinPrice(filter.getMinPrice());
                    info.setMaxPrice(filter.getMaxPrice());
                }
                case "LOT_SIZE" -> info.setStepSize(filter.getStepSize());
            }
        }

        return info;
    }
}
