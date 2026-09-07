package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.AssignmentNotExplicitException;
import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.RoleProvenance;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-AUTH-022 SPEC-AUTH-010: revocación explícita. RN-14: solo revoca asignaciones
 * {@code GRANTED_EXPLICITLY} — una puramente {@code DERIVED_FROM_AD} (o inexistente) se
 * rechaza, orientando al mecanismo correcto (SPEC-AUTH-008 o la próxima sincronización). RN-07:
 * protección transaccional de continuidad de {@code MASTER_ADMIN}, evaluada antes de aplicar
 * cualquier cambio.
 */
@Service
public class RevokeRoleUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public RevokeRoleUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository,
                              RoleRepositoryPort roleRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(UUID actorId, RevokeRoleCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.ROLE_REVOKE, correlationId);

        User user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        UserRoleAssignment assignment = user.findAssignment(command.roleId())
                .filter(a -> a.provenance() == RoleProvenance.GRANTED_EXPLICITLY)
                .orElseThrow(AssignmentNotExplicitException::new); // RN-14: ausente o DERIVED_FROM_AD, mismo rechazo.

        enforceMasterAdminContinuity(user, command.roleId()); // RN-07.

        user.revokeRole(command.roleId()); // RN-15: si era el último rol, el dominio deja PENDING_ONBOARDING.
        userRepository.save(user);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.ROLE_REVOKED, now,
                actorId, user.id(), correlationId, AuditOutcome.SUCCESS,
                Map.of("roleId", assignment.roleId().toString())));
    }

    private void enforceMasterAdminContinuity(User user, UUID roleId) {
        Optional<Role> masterAdminRole = roleRepository.findByName(WellKnownRoles.MASTER_ADMIN);
        boolean isMasterAdminRole = masterAdminRole.isPresent() && masterAdminRole.get().id().equals(roleId);
        boolean userCurrentlyActive = user.status() == UserStatus.ACTIVE;
        if (isMasterAdminRole && userCurrentlyActive && userRepository.countActiveUsersWithRole(roleId) <= 1) {
            throw new MasterAdminContinuityViolationException();
        }
    }
}
