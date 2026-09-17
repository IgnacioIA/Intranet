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
     * INV-AUTH-013, SPEC-AUTH-009 UC-AUTH-014, SPEC-AUTH-010 RN-07 (Fase 17). Nativa: no hay
     * asociación JPA entre esta entidad y {@code UserJpaEntity} (ver Javadoc de
     * {@code UserRoleAssignmentJpaEntity} — clave compuesta gestionada explícitamente, sin
     * relación bidireccional).
     *
     * <p>Usada tanto para "¿hay al menos uno?" ({@code JpaUserRepositoryAdapter#existsActiveUserWithRole}
     * hace {@code > 0} sobre este resultado) como para "¿hay exactamente uno o varios?" (protección
     * del último administrador). No existe una query {@code EXISTS} nativa dedicada: en MySQL,
     * {@code SELECT EXISTS(...)} usado como expresión de columna se tipa como BIGINT, no como
     * booleano, y Hibernate/el driver JDBC lo devuelven como {@code Long} — un método de
     * repositorio declarado {@code boolean} sobre esa query fallaba con
     * {@code ClassCastException: Long cannot be cast to Boolean} al intentar el unboxing.</p>
     */
    @Query(value = "SELECT COUNT(*) FROM user_role_assignments a "
            + "JOIN users u ON u.id = a.user_id WHERE a.role_id = :roleId AND u.status = 'ACTIVE'",
            nativeQuery = true)
    long countActiveUsersWithRole(@Param("roleId") String roleId);
}
