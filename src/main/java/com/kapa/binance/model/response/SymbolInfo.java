package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolInfo {
    private String symbol;
    private int quantityPrecision;
    private int pricePrecision;
    private double tickSize;
    private double stepSize;
    private double minPrice;
    private double maxPrice;
}