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
public class ClosePositionReq {

    @NotBlank(message = "isCopier cannot be blank")
    private Boolean isCopier;

    @NotBlank(message = "uuid cannot be blank")
    private String uuid;

    @NotBlank(message = "symbol cannot be blank")
    private String symbol;

    @NotBlank(message = "positionSide cannot be blank")
    @Pattern(regexp = "LONG|SHORT|long|short", message = "positionSide must be either 'LONG' or 'SHORT' or 'long' or 'short'")
    private String positionSide;

    @NotNull(message = "closePercent cannot be null")
    @Min(value = 20, message = "closePercent must be at least 20 %")
    @Max(value = 100, message = "closePercent must be at most 100 %")
    private Double closePercent;
}
