package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** UC-AUTH-013 SPEC-AUTH-008: `GET /auth/admin/ad-group-mappings`, requiere {@code AD_MAPPING_MANAGE}. */
@Service
public class ListAdGroupMappingsUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final AdGroupRoleMappingRepositoryPort mappingRepository;

    public ListAdGroupMappingsUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                       AdGroupRoleMappingRepositoryPort mappingRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.mappingRepository = mappingRepository;
    }

    public List<AdGroupRoleMapping> handle(UUID actorId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.AD_MAPPING_MANAGE, correlationId);
        return mappingRepository.findAll();
    }
}
