package com.kapa.binance.model.request;

import jakarta.validation.constraints.*;
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

    @NotNull
    @Positive
    @Max(10)
    private Double copierRatio;

    @NotBlank
    private String fullName;
}
