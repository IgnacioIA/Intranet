package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UC-AUTH-020 SPEC-AUTH-010: `GET /auth/admin/permissions/{name}`, requiere {@code PERMISSION_READ}. */
@Service
public class GetPermissionUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final PermissionRepositoryPort permissionRepository;

    public GetPermissionUseCase(AdminActionAuthorizer adminActionAuthorizer, PermissionRepositoryPort permissionRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.permissionRepository = permissionRepository;
    }

    public Permission handle(UUID actorId, String name, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.PERMISSION_READ, correlationId);
        return permissionRepository.findByName(name).orElseThrow(PermissionNotFoundException::new);
    }
}
