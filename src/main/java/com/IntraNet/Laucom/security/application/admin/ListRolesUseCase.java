package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** UC-AUTH-019 SPEC-AUTH-010: `GET /auth/admin/roles`, requiere {@code ROLE_READ}. */
@Service
public class ListRolesUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final RoleRepositoryPort roleRepository;

    public ListRolesUseCase(AdminActionAuthorizer adminActionAuthorizer, RoleRepositoryPort roleRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.roleRepository = roleRepository;
    }

    public List<Role> handle(UUID actorId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_READ, correlationId);
        return roleRepository.findAll();
    }
}
