package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.IdentityManagedExternallyException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** UC-AUTH-017 SPEC-AUTH-010: RN-02, solo aplica a usuarios {@code LOCAL}. */
@Service
public class UpdateLocalUserIdentityUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public UpdateLocalUserIdentityUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                           UserRepositoryPort userRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public User handle(UUID actorId, UpdateLocalUserIdentityCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_MANAGE, correlationId);

        User user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        if (!user.isLocal()) {
            throw new IdentityManagedExternallyException(); // RN-02: solo LOCAL es editable administrativamente.
        }

        user.updateLocalIdentity(command.email(), command.displayName());
        userRepository.save(user);

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.USER_UPDATED, now,
                actorId, user.id(), correlationId, AuditOutcome.SUCCESS, Map.of()));
        return user;
    }
}
