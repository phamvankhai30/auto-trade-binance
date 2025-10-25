package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "log_lever")
public class LogLeverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;

    private Integer lever;

    private String symbol;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
