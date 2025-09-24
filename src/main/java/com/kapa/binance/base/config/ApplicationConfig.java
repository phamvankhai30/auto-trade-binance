package com.kapa.binance.base.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kapa.binance.base.constants.DateConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;

@Slf4j
@Configuration
public class ApplicationConfig {

    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateConstant.INSTANT_FORMAT);
        objectMapper.setDateFormat(simpleDateFormat);
        return objectMapper;
    }
}
