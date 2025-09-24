package com.kapa.binance.controller;

import com.kapa.binance.base.utils.AesEncrypt;
import com.kapa.binance.config.EnvConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final EnvConfig envConfig;

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/encrypted")
    ResponseEntity<?> create(@RequestParam String secretKey) {
        String encode = AesEncrypt.encrypt(secretKey.trim(), envConfig.getSecretKey());
        return ResponseEntity.ok(encode);
    }
}
