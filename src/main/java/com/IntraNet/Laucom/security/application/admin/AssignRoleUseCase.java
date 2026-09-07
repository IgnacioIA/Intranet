package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-021 SPEC-AUTH-010: asignación explícita. El propio dominio ({@code User.assignRole})
 * ya resuelve el upgrade desde {@code DERIVED_FROM_AD} (RN-11, INV-AUTH-015) y la idempotencia
 * sobre una asignación {@code GRANTED_EXPLICITLY} ya existente (RN-12) — este Use Case no
 * duplica esa lógica, solo la orquesta.
 */
@Service
public class AssignRoleUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public AssignRoleUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository,
                              RoleRepositoryPort roleRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public UserRoleAssignment handle(UUID actorId, AssignRoleCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_ASSIGN, correlationId);

        User user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        Role role = roleRepository.findById(command.roleId()).orElseThrow(RoleNotFoundException::new);
        if (!role.isActive()) {
            throw new RoleInactiveException(); // UC-AUTH-021 flujo alternativo.
        }

        Instant now = clock.instant();
        user.assignRole(UserRoleAssignment.grantedExplicitly(role.id(), now), now);
        userRepository.save(user);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.ROLE_ASSIGNED, now,
                actorId, user.id(), correlationId, AuditOutcome.SUCCESS, Map.of("roleId", role.id().toString())));

        return user.findAssignment(role.id()).orElseThrow();
    }
}
