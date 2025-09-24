package com.kapa.binance.base.utils;

import com.kapa.binance.base.dto.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private static CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser) {
            return (CurrentUser) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getUserId() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getUuid() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getUuid() : null;
    }

    public static boolean isActive() {
        CurrentUser user = getCurrentUser();
        return user != null && user.isActive();
    }

    public static String getApiKey() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getApiKey() : null;
    }

    public static String getSecretKey() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getSecretKey() : null;
    }

    public static String getPassPhrase() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getPassPhrase() : null;
    }

    public static Boolean isDemo() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getIsDemo() : true;
    }

    public static String getBaseUrl() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getBaseUrl() : null;
    }

    public static String getRole() {
        CurrentUser user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }
}
