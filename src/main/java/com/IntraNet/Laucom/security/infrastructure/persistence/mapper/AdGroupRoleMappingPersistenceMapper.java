package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.AdGroupRoleMappingJpaEntity;

import java.util.UUID;

public final class AdGroupRoleMappingPersistenceMapper {

    private AdGroupRoleMappingPersistenceMapper() {
    }

    public static AdGroupRoleMapping toDomain(AdGroupRoleMappingJpaEntity entity) {
        return AdGroupRoleMapping.reconstitute(UUID.fromString(entity.getId()), entity.getAdGroupIdentifier(),
                UUID.fromString(entity.getRoleId()), UUID.fromString(entity.getCreatedBy()),
                UUID.fromString(entity.getUpdatedBy()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static AdGroupRoleMappingJpaEntity toNewJpa(AdGroupRoleMapping mapping) {
        return new AdGroupRoleMappingJpaEntity(mapping.id().toString(), mapping.adGroupIdentifier(),
                mapping.roleId().toString(), mapping.createdBy().toString(), mapping.updatedBy().toString(),
                mapping.createdAt(), mapping.updatedAt());
    }
}
