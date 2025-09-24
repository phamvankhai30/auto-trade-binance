package com.kapa.binance.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderRequest {

    private String symbol;
    private String side;
    private String positionSide;
    private String orderType;
    private String timeInForce;
    private double quantity; //unit is token
    private boolean reduceOnly;
    private BigDecimal price;
    private String clientOrderId;
    private BigDecimal stopPrice;
    private boolean closePosition;
    private String workingType;
    private long timestamp;
    private int leverage;
    private double volume;
    private Double stepSize;
    private Integer quantityPrecision;
}
