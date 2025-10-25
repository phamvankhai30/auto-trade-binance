package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClosePositionUserRes {

    private String symbol;
    private String positionSide;
    private double closedQuantity;
    private String uuid;
    private String fullName;
    private String errorMessage;
}
