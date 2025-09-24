package com.kapa.binance.base.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CurrentUser {
    private Long id;
    private String apiKey;
    private String secretKey;
    private String passPhrase;
    private String fullName;
    private boolean isActive;
    private String uuid;
    private Date createdAt;
    private Date updatedAt;
    private Boolean isDemo;
    private String baseUrl;
    private String role;
}
