package com.kapa.binance.base.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapa.binance.model.response.DataOrder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class OrderMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static JsonNode parseEvent(String event) {
        try {
            return objectMapper.readTree(event);
        } catch (IOException e) {
            log.error("Error parsing event {}", event, e);
            return null;
        }
    }

    public static DataOrder mapDataOrder(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            String eventType = root.get("e").asText();
            if (!"ORDER_TRADE_UPDATE".equals(eventType)) {
                return null;
            }

            JsonNode orderNode = root.get("o");
            return mapper.treeToValue(orderNode, DataOrder.class);
        } catch (Exception e) {
            return null;
        }
    }
}