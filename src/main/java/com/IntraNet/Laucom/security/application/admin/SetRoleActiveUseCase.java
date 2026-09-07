package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-019 SPEC-AUTH-010 (activar/desactivar). La protección de {@code MASTER_ADMIN} contra
 * desactivación no se verifica aquí de forma explícita: {@code Role.deactivate()} ya la rechaza
 * incondicionalmente para cualquier Role de sistema (INV-AUTH-011), y {@code MASTER_ADMIN} lo es
 * — verificarlo dos veces sería redundante.
 */
@Service
public class SetRoleActiveUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public SetRoleActiveUseCase(AdminActionAuthorizer adminActionAuthorizer, RoleRepositoryPort roleRepository,
                                 AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public Role handle(UUID actorId, UUID roleId, boolean active, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_MANAGE, correlationId);

        Role role = roleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new);
        if (active) {
            role.activate();
        } else {
            role.deactivate(); // INV-AUTH-011: lanza SystemRoleProtectedException si es de sistema.
        }
        roleRepository.save(role);

        Instant now = clock.instant();
        SecurityEventType eventType = active ? SecurityEventType.ROLE_UPDATED : SecurityEventType.ROLE_DISABLED;
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), eventType, now, actorId, null, correlationId,
                AuditOutcome.SUCCESS, Map.of("roleId", role.id().toString())));
        return role;
    }
}
