package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionResponse {
    private Integer code;
    private String msg;
    private String data;

    public static ConnectionResponse of(Integer code, String msg) {
        return ConnectionResponse.builder()
                .code(code)
                .msg(msg)
                .build();
    }

    public static ConnectionResponse of(Integer code, String msg, String data) {
        return ConnectionResponse.builder()
                .code(code)
                .msg(msg)
                .data(data)
                .build();
    }
}
