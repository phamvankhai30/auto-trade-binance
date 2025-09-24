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
public class PositionInfo {
    private String symbol;
    private Double positionAmt;
    private Double entryPrice;
//    private Double breakEvenPrice;
    private Double markPrice;
    private Double unRealizedProfit;
//    private Double liquidationPrice;
    private Integer leverage;
//    private Long maxNotionalValue;
    private String marginType;
//    private Double isolatedMargin;
//    private Boolean isAutoAddMargin;
    private String positionSide;
    private Double notional;
//    private Long isolatedWallet;
//    private Long updateTime;
//    private Boolean isolated;
//    private Double adlQuantile;
}
