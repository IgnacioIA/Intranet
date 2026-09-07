package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionNameImmutableException;
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

/** UC-AUTH-020 SPEC-AUTH-010 (modificación), RN-10: solo {@code description} es editable. */
@Service
public class UpdatePermissionDescriptionUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final PermissionRepositoryPort permissionRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public UpdatePermissionDescriptionUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                               PermissionRepositoryPort permissionRepository,
                                               AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.permissionRepository = permissionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Permission handle(UUID actorId, UpdatePermissionDescriptionCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.PERMISSION_MANAGE, correlationId);

        if (command.requestedName() != null && !command.requestedName().equals(command.currentName())) {
            throw new PermissionNameImmutableException(); // RN-10.
        }

        Permission permission = permissionRepository.findByName(command.currentName())
                .orElseThrow(PermissionNotFoundException::new);
        permission.updateDescription(command.description());
        permissionRepository.save(permission);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.PERMISSION_UPDATED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS, Map.of("permissionName", permission.name())));
        return permission;
    }
}
