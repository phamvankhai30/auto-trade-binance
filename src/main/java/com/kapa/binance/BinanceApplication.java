package com.kapa.binance;

import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.service.ConnectService;
import com.kapa.binance.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class BinanceApplication implements CommandLineRunner {
    private final EnvConfig envConfig;
    private final ConnectService connectService;
    private final UserService userService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (envConfig.isWsEnable()) {
            List<AuthRequest> user = getFilteredAuthRequests();
            for (AuthRequest authRequest : user) {
                connectService.openConnect(authRequest, false);
            }
        }
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private List<AuthRequest> getFilteredAuthRequests() {
        List<AuthRequest> authRequests = userService.getAllUserAuth();
        if (isDevProfile()) {
            return authRequests.stream()
//                    .filter(authRequest -> StringUtils.equals("123456789", authRequest.getUuid()))
                    .filter(authRequest -> StringUtils.equals("987654321", authRequest.getUuid()))
                    .toList();
        }
        return authRequests;
    }

}
