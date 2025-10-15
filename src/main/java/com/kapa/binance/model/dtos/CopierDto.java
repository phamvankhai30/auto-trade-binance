package com.kapa.binance.model.dtos;

import lombok.Data;

@Data
public class CopierDto {

    private String leaderUuid;
    private String symbol;
    private String positionSide; // LONG, SHORT, BOTH
    private String side; // BUY, SELL
    private Integer leverage;

}
