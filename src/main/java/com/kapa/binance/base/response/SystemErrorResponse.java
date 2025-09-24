package com.kapa.binance.base.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemErrorResponse {
    private @NotBlank String code;
    private @NotBlank String message;
    private Object details;

    public static SystemErrorResponse of(String code, String message) {
        return SystemErrorResponse.builder().code(code).message(message).build();
    }

    public static SystemErrorResponse of(String code, String message, Object details) {
        return SystemErrorResponse.builder().code(code).message(message).details(details).build();
    }
}
