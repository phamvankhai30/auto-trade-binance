package com.kapa.binance.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawSymbolInfo {
    private String symbol;
    private int quantityPrecision;
    private int pricePrecision;
    private List<Filter> filters;
}