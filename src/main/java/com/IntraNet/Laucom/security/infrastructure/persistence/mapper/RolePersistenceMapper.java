package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RoleJpaEntity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * No incluye un {@code toJpa} puro: resolver el conjunto de {@code Permission} a instancias
 * JPA gestionadas requiere acceso al repositorio (para no intentar re-insertar Permissions ya
 * existentes) — esa resolución vive en {@code JpaRoleRepositoryAdapter}, no en este mapper.
 */
public final class RolePersistenceMapper {

    private RolePersistenceMapper() {
    }

    public static Role toDomain(RoleJpaEntity entity) {
        Set<Permission> permissions = entity.getPermissions().stream()
                .map(PermissionPersistenceMapper::toDomain)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Role.reconstitute(UUID.fromString(entity.getId()), entity.getName(), entity.getDescription(),
                entity.isSystemRole(), entity.isActive(), permissions);
    }
}
