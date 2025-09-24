package com.kapa.binance.repository;

import com.kapa.binance.entity.RepeatSymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepeatSymbolRepository extends JpaRepository<RepeatSymbolEntity, Long> {

    RepeatSymbolEntity findFirstByUuidAndSymbolAndPosSideAndIsActiveTrue(String uuid, String symbol, String posSide);
}
