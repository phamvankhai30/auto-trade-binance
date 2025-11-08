package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_uuid", columnList = "uuid"),
        @Index(name = "idx_clientIdParent", columnList = "clientIdParent"),
        @Index(name = "idx_clientIdChildren", columnList = "clientIdChildren"),
        @Index(name = "idx_symbol", columnList = "symbol"),
        @Index(name = "idx_posSide", columnList = "posSide")
})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String posSide;
    private String symbol;
    private Integer step;
    private String clientIdParent;
    private String clientIdChildren;
//    private String mgnMode;
    private Boolean isNew;
    private Boolean isEndStep;
    private Date createAt;
    private Date updatedAt;
    private String uuid;
    private Double volume;
    private Long cTime;
}
