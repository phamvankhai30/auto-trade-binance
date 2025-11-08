//package com.kapa.binance.repository;
//
//import com.kapa.binance.entity.MaxVolSymbolEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface MaxVolSymbolRepository extends JpaRepository<MaxVolSymbolEntity, Long> {
//
//    @Query("SELECT e FROM MaxVolSymbolEntity e WHERE e.uuid = ?1 AND e.symbol = ?2 AND e.longVol > 0")
//    MaxVolSymbolEntity findFirstByUuidAndSymbolAndLongVolGreaterThanZero(String uuid, String symbol);
//
//    @Query("SELECT e FROM MaxVolSymbolEntity e WHERE e.uuid = ?1 AND e.symbol = ?2 AND e.shortVol > 0")
//    MaxVolSymbolEntity findFirstByUuidAndSymbolAndShortVolGreaterThanZero(String uuid, String symbol);
//
//}