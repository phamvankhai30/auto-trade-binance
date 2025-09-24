package com.kapa.binance.base.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderInfo {
    private String clientOrderId;
    private String side;
    private String positionSide;
    private String symbol;
    private String status;
    private double avgPrice;
    private double origQty;
    private long updateTime;
    /*
    private long orderId;
    private String price;
    private String executedQty;
    private String cumQuote;
    private String timeInForce;
    private String type;
    private boolean reduceOnly;
    private boolean closePosition;
    private String stopPrice;
    private String workingType;
    private boolean priceProtect;
    private String origType;
    private String priceMatch;
    private String selfTradePreventionMode;
    private long goodTillDate;
    private long time;
    */
}
