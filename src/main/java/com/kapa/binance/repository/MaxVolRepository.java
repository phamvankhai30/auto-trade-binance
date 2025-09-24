package com.kapa.binance.repository;

import com.kapa.binance.entity.MaxVolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaxVolRepository extends JpaRepository<MaxVolEntity, Long> {

    MaxVolEntity findFirstByUuidOrderByIdDesc(String uuid);
}