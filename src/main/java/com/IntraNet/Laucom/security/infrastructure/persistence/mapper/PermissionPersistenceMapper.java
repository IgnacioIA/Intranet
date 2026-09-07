package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PermissionJpaEntity;

public final class PermissionPersistenceMapper {

    private PermissionPersistenceMapper() {
    }

    public static PermissionJpaEntity toNewJpa(Permission permission) {
        return new PermissionJpaEntity(permission.name(), permission.description(),
                permission.isSystemPermission(), permission.isActive());
    }

    public static Permission toDomain(PermissionJpaEntity entity) {
        return Permission.reconstitute(entity.getName(), entity.getDescription(),
                entity.isSystemPermission(), entity.isActive());
    }
}
