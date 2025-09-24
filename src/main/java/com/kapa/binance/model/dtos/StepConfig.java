package com.kapa.binance.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepConfig {
    private int step;
    private Double roi;
    private Double usdt;
    private Double takeProfit;
    private Double stopLoss;
    private Integer lever;
    private Double quantityDropPercent;
    private Double priceDropPercent;
}