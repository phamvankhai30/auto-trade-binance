package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "steps", indexes = {
        @Index(name = "idx_uuid", columnList = "uuid"),
        @Index(name = "idx_step", columnList = "step")
})
public class StepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
