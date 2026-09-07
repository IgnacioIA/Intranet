package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Fase 18 (Soft Deactivation, verificación): mismo criterio que {@link RoleJpaRepository} —
 * extiende el marcador {@link Repository}, sin ningún método de eliminación física, reforzando
 * INV-AUTH-014 en tiempo de compilación.
 */
public interface PermissionJpaRepository extends Repository<PermissionJpaEntity, String> {

    Optional<PermissionJpaEntity> findById(String name);

    List<PermissionJpaEntity> findAll();

    PermissionJpaEntity save(PermissionJpaEntity entity);
}
