package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;

/** SPEC-AUTH-008 §12. */
public record AdGroupRoleMappingResponse(String id, String adGroupIdentifier, String roleId, String updatedAt) {

    public static AdGroupRoleMappingResponse from(AdGroupRoleMapping mapping) {
        return new AdGroupRoleMappingResponse(mapping.id().toString(), mapping.adGroupIdentifier(),
                mapping.roleId().toString(), mapping.updatedAt().toString());
    }
}
