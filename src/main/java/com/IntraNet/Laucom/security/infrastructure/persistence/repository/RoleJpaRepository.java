package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Fase 18 (Soft Deactivation, verificación): extiende el marcador {@link Repository} en vez de
 * {@code JpaRepository}, y declara explícitamente solo los métodos que el adapter usa — sin
 * {@code delete}/{@code deleteById}/{@code deleteAll}. INV-AUTH-014 (Role/Permission nunca se
 * eliminan físicamente) queda así reforzada en tiempo de compilación, no solo por disciplina de
 * código: ningún código futuro puede invocar una eliminación física de {@code Role} a través de
 * este repositorio aunque lo intente, porque el método simplemente no existe en esta interfaz.
 */
public interface RoleJpaRepository extends Repository<RoleJpaEntity, String> {

    Optional<RoleJpaEntity> findById(String id);

    Optional<RoleJpaEntity> findByName(String name);

    List<RoleJpaEntity> findAll();

    RoleJpaEntity save(RoleJpaEntity entity);
}
