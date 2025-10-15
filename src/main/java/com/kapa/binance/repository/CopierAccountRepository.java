package com.kapa.binance.repository;

import com.kapa.binance.entity.CopierAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopierAccountRepository extends JpaRepository<CopierAccountEntity, Long> {

    @Query("""
            SELECT c
            FROM CopierAccountEntity c
                JOIN UserEntity u ON c.leaderUuid = u.uuid
                    WHERE c.leaderUuid = :leaderUuid
                        AND c.isActive = true
                        AND u.isAllowCopy = true
            """)
    List<CopierAccountEntity> findActiveCopiersByLeader(@Param("leaderUuid") String leaderUuid);

    boolean existsByCopierUuidOrAndApiKey(String copierUuid, String apiKey);

    Optional<CopierAccountEntity> findByCopierUuid(String copierUuid);
}