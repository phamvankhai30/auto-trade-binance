package com.kapa.binance.repository;

import com.kapa.binance.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u where u.isActive = true")
    List<UserEntity> findAllByActiveIsTrue();

    UserEntity findFirstByUuid(String uuid);

    @Transactional
    @Modifying
    @Query("UPDATE UserEntity u SET u.connectStatus = ?2 WHERE u.uuid in ?1")
    void updateIsConnectedByUuid(List<String> uuid, String connectStatus);

    Optional<UserEntity> findByUuid(String uuid);
}
