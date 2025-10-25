package com.kapa.binance.repository;

import com.kapa.binance.entity.LogLeverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogLeverRepository extends JpaRepository<LogLeverEntity, Long> {

    Optional<LogLeverEntity> findByUuidAndSymbol(String uuid, String symbol);
}