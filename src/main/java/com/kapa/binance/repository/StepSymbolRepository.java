package com.kapa.binance.repository;

import com.kapa.binance.entity.StepSymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StepSymbolRepository extends JpaRepository<StepSymbolEntity, Long> {

    StepSymbolEntity findFirstByUuidAndSymbol(String uuid, String symbol);

    StepSymbolEntity findFirstByUuidAndSymbolAndStep(String uuid, String symbol, Integer step);
}
