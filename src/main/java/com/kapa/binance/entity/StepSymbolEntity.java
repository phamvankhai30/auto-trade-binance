package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "step_symbol")
public class StepSymbolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private Integer step;
    private Double roi;
    private Double usdt;
    private Double takeProfit;
    private Double stopLoss;
    private Integer lever;
    private Double quantityDropPercent;
    private Double priceDropPercent;
    private String uuid;
}
