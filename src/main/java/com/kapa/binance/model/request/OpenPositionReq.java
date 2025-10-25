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
public class OpenPositionReq {

    @NotBlank(message = "uuid cannot be blank")
    private String uuid;

    @NotBlank(message = "symbol cannot be blank")
    private String symbol;

    @NotBlank(message = "positionSide cannot be blank")
    @Pattern(regexp = "LONG|SHORT|long|short", message = "positionSide must be either 'LONG' or 'SHORT' or 'long' or 'short'")
    private String positionSide;

    @NotNull(message = "usdt cannot be null")
    @Min(value = 5, message = "usdt must be at least 5")
    @Max(value = 1000, message = "usdt must be at most 1000")
    private Double usdt;

    @Min(1)
    @Max(125)
    @NotNull(message = "leverage cannot be null")
    private Integer leverage;
}
