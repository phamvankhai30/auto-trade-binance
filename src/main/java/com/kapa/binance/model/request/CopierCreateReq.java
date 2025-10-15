package com.kapa.binance.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopierCreateReq {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String copierUuid;

    @NotBlank
    private String leaderUuid;

    @NotNull
    private Boolean isActive;
}
