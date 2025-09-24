package com.kapa.binance.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
public class EnvConfig {

    @Value("${config.binance.base-api}")
    private String baseApi;

    @Value("${config.binance.base-ws-api}")
    private String baseWsApi;

    @Value("${config.app.secret-key}")
    private String secretKey;

    @Value("${config.ws.enable}")
    private boolean wsEnable;

    @Value("${config.security.enable:true}")
    private boolean enabledSecurity;

    @Value("${config.security.whitelist}")
    private List<String> whitelist;

}
