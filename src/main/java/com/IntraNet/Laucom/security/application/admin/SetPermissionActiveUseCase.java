package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-020 SPEC-AUTH-010 (activar/desactivar). {@code Permission.deactivate()} ya rechaza
 * incondicionalmente cualquier permiso de sistema (ej. {@code VIEW_ONBOARDING_INFO}).
 */
@Service
public class SetPermissionActiveUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final PermissionRepositoryPort permissionRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public SetPermissionActiveUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                       PermissionRepositoryPort permissionRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.permissionRepository = permissionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Permission handle(UUID actorId, String name, boolean active, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.PERMISSION_MANAGE, correlationId);

        Permission permission = permissionRepository.findByName(name).orElseThrow(PermissionNotFoundException::new);
        if (active) {
            permission.activate();
        } else {
            permission.deactivate(); // RN-09: lanza SystemPermissionProtectedException si es de sistema.
        }
        permissionRepository.save(permission);

        Instant now = clock.instant();
        SecurityEventType eventType = active ? SecurityEventType.PERMISSION_UPDATED : SecurityEventType.PERMISSION_DISABLED;
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), eventType, now, actorId, null, correlationId,
                AuditOutcome.SUCCESS, Map.of("permissionName", permission.name())));
        return permission;
    }
}
