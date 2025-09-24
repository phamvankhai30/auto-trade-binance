package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "repeat_symbol")
public class RepeatSymbolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private String posSide;
    private Boolean isActive;
    private Double volume;
    private Integer repeatCount;
    private String uuid;
}
