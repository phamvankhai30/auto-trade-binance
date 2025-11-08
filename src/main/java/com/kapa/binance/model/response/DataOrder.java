package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataOrder {

    @JsonProperty("s")
    private String symbol; // Symbol

    @JsonProperty("c")
    private String clientOrderId; // Client Order Id

    @JsonProperty("S")
    private String side; // Side

    @JsonProperty("o")
    private String orderType; // Order Type

    @JsonProperty("f")
    private String timeInForce; // Time in Force

    @JsonProperty("q")
    private Double originalQuantity; // Original Quantity

    @JsonProperty("p")
    private String originalPrice; // Original Price

    @JsonProperty("ap")
    private String averagePrice; // Average Price

    @JsonProperty("sp")
    private String stopPrice; // Stop Price

    @JsonProperty("x")
    private String executionType; // Execution Type

    @JsonProperty("X")
    private String orderStatus; // Order Status

    @JsonProperty("i")
    private String orderId; // Order Id

    @JsonProperty("l")
    private String lastFilledQuantity; // Order Last Filled Quantity

    @JsonProperty("z")
    private String filledAccumulatedQuantity; // Order Filled Accumulated Quantity

    @JsonProperty("L")
    private String lastFilledPrice; // Last Filled Price

    @JsonProperty("N")
    private String commissionAsset; // Commission Asset

    @JsonProperty("n")
    private String commission; // Commission

    @JsonProperty("T")
    private Long orderTradeTime; // Order Trade Time

    @JsonProperty("t")
    private Long tradeId; // Trade Id

    @JsonProperty("m")
    private Boolean isMaker; // Is this trade the maker side?

    @JsonProperty("R")
    private Boolean isReduceOnly; // Is this reduce only

    @JsonProperty("wt")
    private String stopPriceWorkingType; // Stop Price Working Type

    @JsonProperty("ot")
    private String originalOrderType; // Original Order Type

    @JsonProperty("ps")
    private String positionSide; // Position Side

    @JsonProperty("cp")
    private Boolean isCloseAll; // If Close-All, pushed with conditional order

    @JsonProperty("rp")
    private Double realizedProfit; // Realized Profit of the trade

    @JsonProperty("pP")
    private Boolean isPriceProtectionOn; // If price protection is turned on

    @JsonProperty("V")
    private String stpMode; // STP mode

    @JsonProperty("pm")
    private String priceMatchMode; // Price match mode

    @JsonProperty("gtd")
    private Long autoCancelTime; // TIF GTD order auto cancel time
}
