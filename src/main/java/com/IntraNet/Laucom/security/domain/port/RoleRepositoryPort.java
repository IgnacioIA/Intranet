package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del agregado {@link Role}. Ver architecture.md, entities.md "Role".
 * Agregado en Fase 2 (Persistencia) porque esa fase requiere explícitamente que la
 * autorización pueda consultar los permisos vigentes de un usuario en DB — no es especulativo.
 */
public interface RoleRepositoryPort {

    Optional<Role> findById(UUID id);

    /** INV-AUTH-010/entities.md: `name` es único. */
    Optional<Role> findByName(String name);

    /** UC-AUTH-019 SPEC-AUTH-010: `GET /auth/admin/roles`. Catálogo acotado, sin paginación. */
    List<Role> findAll();

    Role save(Role role);
}
