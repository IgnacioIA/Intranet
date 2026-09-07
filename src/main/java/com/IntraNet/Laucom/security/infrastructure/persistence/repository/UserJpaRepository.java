package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByProviderAndUsername(String provider, String username);

    Optional<UserJpaEntity> findByExternalId(String externalId);

    Optional<UserJpaEntity> findByProviderAndEmail(String provider, String email);

    /** UC-AUTH-015 SPEC-AUTH-010: cada filtro es opcional (NULL desactiva esa condición). */
    @Query(value = "SELECT u FROM UserJpaEntity u WHERE "
            + "(:status IS NULL OR u.status = :status) AND "
            + "(:provider IS NULL OR u.provider = :provider) AND "
            + "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(u) FROM UserJpaEntity u WHERE "
                    + "(:status IS NULL OR u.status = :status) AND "
                    + "(:provider IS NULL OR u.provider = :provider) AND "
                    + "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) "
                    + "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')) "
                    + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserJpaEntity> search(@Param("status") String status, @Param("provider") String provider,
                                @Param("search") String search, Pageable pageable);
}
