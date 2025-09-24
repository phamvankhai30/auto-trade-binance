package com.kapa.binance.repository;

import com.kapa.binance.entity.StepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StepRepository extends JpaRepository<StepEntity, Long> {

    StepEntity findFirstByUuidAndStep(String uuid, int step);
}
