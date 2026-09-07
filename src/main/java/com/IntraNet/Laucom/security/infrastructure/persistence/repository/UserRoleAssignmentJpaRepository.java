package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserRoleAssignmentId;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserRoleAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleAssignmentJpaRepository extends JpaRepository<UserRoleAssignmentJpaEntity, UserRoleAssignmentId> {

    List<UserRoleAssignmentJpaEntity> findByIdUserId(String userId);

    void deleteByIdUserId(String userId);

    /**
     * INV-AUTH-013, SPEC-AUTH-009 UC-AUTH-014. Nativa: no hay asociación JPA entre esta entidad
     * y {@code UserJpaEntity} (ver Javadoc de {@code UserRoleAssignmentJpaEntity} — clave
     * compuesta gestionada explícitamente, sin relación bidireccional).
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM user_role_assignments a "
            + "JOIN users u ON u.id = a.user_id WHERE a.role_id = :roleId AND u.status = 'ACTIVE')",
            nativeQuery = true)
    boolean existsActiveUserWithRole(@Param("roleId") String roleId);

    /** INV-AUTH-013, SPEC-AUTH-010 RN-07 (Fase 17): distingue "exactamente uno" de "hay varios". */
    @Query(value = "SELECT COUNT(*) FROM user_role_assignments a "
            + "JOIN users u ON u.id = a.user_id WHERE a.role_id = :roleId AND u.status = 'ACTIVE'",
            nativeQuery = true)
    long countActiveUsersWithRole(@Param("roleId") String roleId);
}
