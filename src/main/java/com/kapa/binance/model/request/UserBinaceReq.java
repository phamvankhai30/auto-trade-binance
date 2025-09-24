package com.kapa.binance.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBinaceReq {
    @NotBlank
    private String apiKey;
    @NotBlank
    private String secretKey;
    @NotBlank
    private String passPhrase;

    private String fullName;
}
