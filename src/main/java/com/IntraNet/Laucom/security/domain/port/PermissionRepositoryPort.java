package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Persistencia del Value Object {@link Permission}, identificado por su {@code name}
 * (inmutable). Ver entities.md "Permission". Agregado en Fase 2 por el mismo motivo que
 * {@link RoleRepositoryPort}.
 */
public interface PermissionRepositoryPort {

    Optional<Permission> findByName(String name);

    /** UC-AUTH-020 SPEC-AUTH-010: `GET /auth/admin/permissions`. */
    List<Permission> findAll();

    Permission save(Permission permission);
}
