package com.kapa.binance.base.sercurity;

import com.kapa.binance.base.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service("authService")
public class AuthService {

    @Value("${config.security.enable:true}")
    private boolean enabledSecurity;
    @Value("${config.security.check-role:true}")
    private boolean enabledCheckAction;

    public boolean authorize(String roleCode) {
        if (!enabledSecurity || !enabledCheckAction) return true;
        try {
            String role = SecurityUtils.getRole();
            return StringUtils.equals(role, roleCode);
        } catch (Exception e) {
            log.error("Authorization error: {}", e.getMessage(), e);
            return false;
        }
    }
}
