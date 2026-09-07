package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-018 SPEC-AUTH-010: cambio de estado administrativo (enable/disable/lock/unlock/
 * deprovision), con la protección transaccional de continuidad de {@code MASTER_ADMIN}
 * (RN-07, INV-AUTH-013) evaluada antes de aplicar cualquier cambio — nunca como alerta posterior.
 *
 * <p>Las transiciones de estado inválidas (RN-03) las rechaza el propio dominio
 * ({@code InvalidUserStateTransitionException}, ya lanzada por {@code User.enable/disable/...}):
 * no se duplica esa validación aquí.</p>
 */
@Service
public class ChangeUserStatusUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final SessionRevocationPort sessionRevocation;
    private final AuditPort auditPort;
    private final Clock clock;

    public ChangeUserStatusUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository,
                                    RoleRepositoryPort roleRepository, SessionRevocationPort sessionRevocation,
                                    AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRevocation = sessionRevocation;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public User handle(UUID actorId, ChangeUserStatusCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_MANAGE, correlationId);

        User user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        Instant now = clock.instant();
        AdminUserStatusOperation operation = command.operation();

        if (operation.requiresMasterAdminContinuityCheck()) {
            enforceMasterAdminContinuity(user); // RN-07: rechazo transaccional y bloqueante, antes de aplicar nada.
        }

        applyTransition(user, operation, now); // RN-03: transición inválida ya la rechaza el dominio.

        if (operation.revokesSessions()) {
            sessionRevocation.revokeAllSessions(user.id()); // RN-04.
        }

        userRepository.save(user);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), operation.auditEventType(), now,
                actorId, user.id(), correlationId, AuditOutcome.SUCCESS, Map.of()));
        return user;
    }

    private void applyTransition(User user, AdminUserStatusOperation operation, Instant now) {
        switch (operation) {
            case ENABLE -> user.enable(now); // RN-05: el propio dominio reevalúa roles, no asume ACTIVE ciegamente.
            case DISABLE -> user.disable(now);
            case LOCK -> user.lock(now);
            case UNLOCK -> user.unlock(now);
            case DEPROVISION -> user.deprovision(now); // RN-06: terminal.
        }
    }

    private void enforceMasterAdminContinuity(User user) {
        Role masterAdminRole = roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "El Role de sistema MASTER_ADMIN no existe — falta aplicar la migración V5"));

        boolean userHoldsIt = user.findAssignment(masterAdminRole.id()).isPresent();
        boolean userCurrentlyActive = user.status() == UserStatus.ACTIVE;
        if (userHoldsIt && userCurrentlyActive && userRepository.countActiveUsersWithRole(masterAdminRole.id()) <= 1) {
            throw new MasterAdminContinuityViolationException();
        }
    }
}
