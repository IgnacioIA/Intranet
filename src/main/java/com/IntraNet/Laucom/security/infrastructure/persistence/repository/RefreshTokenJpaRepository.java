package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    /** INV-AUTH-007: revocación masiva de toda una familia ante reutilización detectada. */
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revokedAt = :revokedAt "
            + "WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    void revokeAllActiveInFamily(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    /** SessionRevocationPort: revocación masiva de todas las sesiones vigentes de un usuario. */
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revokedAt = :revokedAt "
            + "WHERE t.userId = :userId AND t.revokedAt IS NULL")
    void revokeAllActiveForUser(@Param("userId") String userId, @Param("revokedAt") Instant revokedAt);
}
