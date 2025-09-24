package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "max_vol")
public class MaxVolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double bothVol;
    private Double longVol;
    private Double shortVol;
    private String uuid;
}
