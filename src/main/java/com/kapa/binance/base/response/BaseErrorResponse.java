package com.kapa.binance.base.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseErrorResponse {
    private String code;
    private String message;
    private String property;
    private String parameterName;
    private String parameterType;

    public static BaseErrorResponse of(String code, String message, String property) {
        return BaseErrorResponse.builder()
                .code(code)
                .message(message)
                .property(property)
                .build();
    }
}