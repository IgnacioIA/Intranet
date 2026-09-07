package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionAlreadyExistsException;
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

/** UC-AUTH-020 SPEC-AUTH-010 (alta), requiere {@code PERMISSION_MANAGE}. */
@Service
public class CreatePermissionUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final PermissionRepositoryPort permissionRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreatePermissionUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                    PermissionRepositoryPort permissionRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.permissionRepository = permissionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Permission handle(UUID actorId, CreatePermissionCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.PERMISSION_MANAGE, correlationId);

        if (permissionRepository.findByName(command.name()).isPresent()) {
            throw new PermissionAlreadyExistsException();
        }

        Permission permission = Permission.create(command.name(), command.description());
        permissionRepository.save(permission);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.PERMISSION_CREATED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS, Map.of("permissionName", command.name())));
        return permission;
    }
}
