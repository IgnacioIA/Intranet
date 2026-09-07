package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.application.exception.RoleAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** UC-AUTH-019 SPEC-AUTH-010 (alta), requiere {@code ROLE_MANAGE}. */
@Service
public class CreateRoleUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreateRoleUseCase(AdminActionAuthorizer adminActionAuthorizer, RoleRepositoryPort roleRepository,
                              PermissionRepositoryPort permissionRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Role handle(UUID actorId, CreateRoleCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_MANAGE, correlationId);

        if (roleRepository.findByName(command.name()).isPresent()) {
            throw new RoleAlreadyExistsException();
        }

        Role role = Role.create(UUID.randomUUID(), command.name(), command.description());
        for (Permission permission : resolvePermissions(command.permissionNames())) {
            role.grant(permission);
        }
        roleRepository.save(role);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.ROLE_CREATED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS, Map.of("roleId", role.id().toString())));
        return role;
    }

    private Set<Permission> resolvePermissions(Set<String> permissionNames) {
        if (permissionNames == null) {
            return Set.of();
        }
        return permissionNames.stream()
                .map(name -> permissionRepository.findByName(name).orElseThrow(PermissionNotFoundException::new))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
