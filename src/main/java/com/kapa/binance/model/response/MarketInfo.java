package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketInfo {
    private String symbol;

    @JsonProperty("markPrice")
    private double markPrice;

//    @JsonProperty("indexPrice")
//    private String indexPrice;
//
//    @JsonProperty("estimatedSettlePrice")
//    private String estimatedSettlePrice;
//
//    @JsonProperty("lastFundingRate")
//    private String lastFundingRate;
//
//    @JsonProperty("interestRate")
//    private String interestRate;
//
//    @JsonProperty("nextFundingTime")
//    private long nextFundingTime;
//
//    private long time;
}
