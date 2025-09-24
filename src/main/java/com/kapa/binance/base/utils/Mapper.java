package com.kapa.binance.base.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Mapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String dtoToJson(Object obj) {
        try {
            if (obj == null) return null;
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
