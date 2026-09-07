package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
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
import java.util.stream.Collectors;

/**
 * UC-AUTH-019 SPEC-AUTH-010 (modificación), requiere {@code ROLE_MANAGE}. Reemplaza la
 * {@code description} y calcula la diferencia entre el conjunto de permisos vigente y el
 * solicitado (otorga los nuevos, retira los que ya no corresponden) — el propio dominio
 * ({@code Role.revoke}) rechaza dejar a un Role de sistema sin permisos (INV-AUTH-011).
 */
@Service
public class UpdateRoleUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public UpdateRoleUseCase(AdminActionAuthorizer adminActionAuthorizer, RoleRepositoryPort roleRepository,
                              PermissionRepositoryPort permissionRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Role handle(UUID actorId, UpdateRoleCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_MANAGE, correlationId);

        Role role = roleRepository.findById(command.roleId()).orElseThrow(RoleNotFoundException::new);
        role.updateDescription(command.description());

        if (command.permissionNames() != null) {
            Set<Permission> desired = command.permissionNames().stream()
                    .map(name -> permissionRepository.findByName(name).orElseThrow(PermissionNotFoundException::new))
                    .collect(Collectors.toUnmodifiableSet());

            Set<Permission> current = role.permissions();
            current.stream().filter(p -> !desired.contains(p)).forEach(role::revoke);
            desired.stream().filter(p -> !current.contains(p)).forEach(role::grant);
        }

        roleRepository.save(role);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.ROLE_UPDATED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS, Map.of("roleId", role.id().toString())));
        return role;
    }
}
