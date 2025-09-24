package com.kapa.binance.model.dtos;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
    private String apiKey;
    private String secretKey;
    private String uuid;
    private Boolean isActive;
}
