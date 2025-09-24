package com.kapa.binance.repository;

import com.kapa.binance.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    boolean existsByUuidAndClientIdParentOrClientIdChildren(String uuid, String clientIdParent, String clientIdChildren);

    OrderEntity findFirstByUuidAndClientIdChildren(String uuid, String clientIdChildren);
}
