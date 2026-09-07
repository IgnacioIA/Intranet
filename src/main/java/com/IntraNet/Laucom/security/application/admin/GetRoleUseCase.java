package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UC-AUTH-019 SPEC-AUTH-010: `GET /auth/admin/roles/{roleId}`, requiere {@code ROLE_READ}. */
@Service
public class GetRoleUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final RoleRepositoryPort roleRepository;

    public GetRoleUseCase(AdminActionAuthorizer adminActionAuthorizer, RoleRepositoryPort roleRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.roleRepository = roleRepository;
    }

    public Role handle(UUID actorId, UUID roleId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_READ, correlationId);
        return roleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new);
    }
}
