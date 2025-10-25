package com.kapa.binance.controller;

import com.kapa.binance.base.exception.Ex422;
import com.kapa.binance.base.utils.AesEncrypt;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.entity.CopierAccountEntity;
import com.kapa.binance.entity.UserEntity;
import com.kapa.binance.model.request.CopierCreateReq;
import com.kapa.binance.repository.CopierAccountRepository;
import com.kapa.binance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final EnvConfig envConfig;
    private final CopierAccountRepository copierAccountRepository;
    private final UserRepository userRepository;

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/encrypted")
    ResponseEntity<?> encrypt(@RequestParam String secretKey) {
        String encode = AesEncrypt.encrypt(secretKey.trim(), envConfig.getSecretKey());
        return ResponseEntity.ok(encode);
    }

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/copier/create")
    ResponseEntity<?> create(@RequestBody CopierCreateReq req) {

        boolean exists = copierAccountRepository.existsByCopierUuidOrAndApiKey(req.getCopierUuid().trim(), req.getApiKey().trim());
        if (exists) {
            throw new Ex422("Copier UUID or API Key already exists");
        }

        String encode = AesEncrypt.encrypt(req.getSecretKey().trim(), envConfig.getSecretKey());
        CopierAccountEntity entity = CopierAccountEntity.builder()
                .apiKey(req.getApiKey().trim())
                .secretKey(encode)
                .copierUuid(req.getCopierUuid().trim())
                .leaderUuid(req.getLeaderUuid().trim())
                .isActive(req.getIsActive())
                .copierRatio(req.getCopierRatio())
                .fullName(req.getFullName().trim())
                .createdAt(LocalDateTime.now())
                .build();
        copierAccountRepository.save(entity);
        return ResponseEntity.ok(entity);
    }

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/copier/enable")
    ResponseEntity<?> enableCopier(@RequestParam String copierUuid, @RequestParam Boolean isEnable) {
        CopierAccountEntity c = copierAccountRepository.findByCopierUuid(copierUuid.trim())
                .orElseThrow(() -> new Ex422("Copier UUID not found"));
        c.setIsActive(isEnable);
        copierAccountRepository.save(c);
        return ResponseEntity.ok("Success");
    }

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/leader/enable")
    ResponseEntity<?> enableLeader(@RequestParam String leaderUuid, @RequestParam Boolean isEnable) {
        UserEntity u = userRepository.findByUuid(leaderUuid.trim())
                .orElseThrow(() -> new Ex422("Leader UUID not found"));
        u.setIsAllowCopy(isEnable);
        userRepository.save(u);
        return ResponseEntity.ok("Success");
    }

}
