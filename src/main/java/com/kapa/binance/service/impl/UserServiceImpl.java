package com.kapa.binance.service.impl;

import com.kapa.binance.base.utils.AesEncrypt;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.entity.UserEntity;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.repository.UserRepository;
import com.kapa.binance.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final EnvConfig envConfig;
    private final UserRepository userRepository;

    @Async
    @Override
    public void updateStatusConnectAsync(List<String> uuid, String connectStatus) {
        log.info("Start updateStatusConnectAsync for UUIDs: {} {}", uuid, connectStatus);
        if (uuid == null || uuid.isEmpty()) return;
        userRepository.updateIsConnectedByUuid(uuid, connectStatus);
    }

    @Override
    public void updateStatusConnectSync(List<String> uuid, String connectStatus) {
        log.info("Updating connection status for UUIDs: {} {}", uuid, connectStatus);
        if (uuid == null || uuid.isEmpty()) return;
        userRepository.updateIsConnectedByUuid(uuid, connectStatus);
    }

    @Override
    public List<AuthRequest> getAllUserAuth() {
        List<UserEntity> users = userRepository.findAllByActiveIsTrue();
        String secretKey = envConfig.getSecretKey();
        List<AuthRequest> authRequests = new ArrayList<>();
        for (UserEntity user : users) {
           try {
               String apiKey = user.getApiKey();
               String apiSecret = user.getSecretKey();

               AuthRequest auth = new AuthRequest();
               auth.setApiKey(apiKey);
               auth.setUuid(user.getUuid());
               auth.setSecretKey(AesEncrypt.decrypt(apiSecret, secretKey));
               auth.setIsActive(user.getIsActive());
               authRequests.add(auth);
           } catch (Exception e) {
               log.error("Error getAllUserAuth uuid {}: {}", user.getUuid(), e.getMessage());
           }
        }
        return authRequests;
    }

    @Override
    public AuthRequest getUserAuthByUuid(String uuid) {
        try {
            UserEntity user = userRepository.findFirstByUuid(uuid);
            if (user == null) return null;

            String apiKey = user.getApiKey();
            String apiSecret = user.getSecretKey();

            return AuthRequest.builder()
                    .apiKey(apiKey)
                    .secretKey(AesEncrypt.decrypt(apiSecret, envConfig.getSecretKey()))
                    .uuid(user.getUuid())
                    .isActive(user.getIsActive())
                    .build();
        } catch (Exception e) {
            log.error("Error getUserAuthByUuid uuid {}: {}", uuid, e.getMessage());
            return null;
        }
    }
}
