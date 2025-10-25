package com.kapa.binance.service.impl;

import com.kapa.binance.base.utils.AesEncrypt;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.entity.CopierAccountEntity;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.repository.CopierAccountRepository;
import com.kapa.binance.service.CopierAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopierAccountServiceImpl implements CopierAccountService {

    private final EnvConfig envConfig;
    private final CopierAccountRepository copierAccountRepository;

    @Override
    public List<AuthRequest> getCopierAuthByLeader(String leaderUuid) {
        List<CopierAccountEntity> copiers = copierAccountRepository.findActiveCopiersByLeader(leaderUuid);

        String secretKey = envConfig.getSecretKey();
        List<AuthRequest> auth = new ArrayList<>();
        for (CopierAccountEntity c : copiers) {
            try {
                String apiKey = c.getApiKey();
                String apiSecret = c.getSecretKey();

                AuthRequest a = new AuthRequest();
                a.setApiKey(apiKey);
                a.setUuid(c.getCopierUuid());
                a.setSecretKey(AesEncrypt.decrypt(apiSecret, secretKey));
                a.setIsActive(c.getIsActive());
                a.setCopierRatio(c.getCopierRatio());
                auth.add(a);
            } catch (Exception e) {
                log.error("Error getCopierAuth copier uuid {}: {}", c.getCopierUuid(), e.getMessage());
            }
        }
        return auth;
    }
}
