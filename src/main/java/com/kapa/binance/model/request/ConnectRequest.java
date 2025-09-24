package com.kapa.binance.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConnectRequest {

    @Size(max = 100)
    @NotBlank
    private String uuid;
}