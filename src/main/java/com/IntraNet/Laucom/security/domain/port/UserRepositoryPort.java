package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del agregado {@link User}. Ver architecture.md, entities.md "User".
 * Ampliada en fases posteriores (ej. conteo para INV-AUTH-013 en Fase 12/17) solo cuando un
 * Use Case concreto lo requiera — no se anticipan métodos especulativos aquí.
 */
public interface UserRepositoryPort {

    Optional<User> findById(UUID id);

    /** INV-AUTH-001: unicidad de (provider, username). */
    Optional<User> findByProviderAndUsername(IdentityProvider provider, String username);

    /** INV-AUTH-002: unicidad de (provider, externalId) para ACTIVE_DIRECTORY. */
    Optional<User> findByExternalId(String externalId);

    /** SPEC-AUTH-007 UC-AUTH-011: recuperación de contraseña busca por (provider, email). */
    Optional<User> findByProviderAndEmail(IdentityProvider provider, String email);

    /**
     * INV-AUTH-013, SPEC-AUTH-009 UC-AUTH-014 (Fase 12): ¿existe al menos un {@code User} con
     * {@code status = ACTIVE} que posea el rol indicado? Usado tanto para la verificación de
     * idempotencia del bootstrap del Master Admin como, más adelante (Fase 17), para la
     * protección transaccional del último administrador.
     */
    boolean existsActiveUserWithRole(UUID roleId);

    /**
     * INV-AUTH-013, SPEC-AUTH-010 RN-07 (Fase 17): cuántos {@code User} {@code ACTIVE} poseen
     * el rol indicado — a diferencia de {@link #existsActiveUserWithRole}, necesario aquí
     * porque la protección del último administrador exige distinguir "hay exactamente uno" de
     * "hay varios", no solo "hay al menos uno".
     */
    long countActiveUsersWithRole(UUID roleId);

    /** UC-AUTH-015 SPEC-AUTH-010: `GET /auth/admin/users`, con filtros y paginación. */
    PageResult<User> search(UserSearchCriteria criteria);

    User save(User user);
}
