package com.kapa.binance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String apiKey;
    private String secretKey;
    private String fullName;
    private Boolean isActive;
    private String uuid;
    private Date createdAt;
    private Date updatedAt;
    private Integer reasonCode;
    private String bio;
    private String role;
    private String connectStatus;
    private String passPhrase;
}