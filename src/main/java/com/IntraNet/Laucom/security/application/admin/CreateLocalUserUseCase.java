package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.application.exception.UsernameAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import com.IntraNet.Laucom.security.domain.service.PasswordPolicy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-016 SPEC-AUTH-010: alta de un usuario {@code LOCAL} (RN-01: nunca {@code ACTIVE_DIRECTORY}).
 * Mismo patrón de credencial inicial que el bootstrap del Master Admin (SPEC-AUTH-009):
 * {@code mustChangeOnNextLogin = true}. Sin rol por defecto — queda en {@code PENDING_ONBOARDING}
 * hasta una asignación explícita (UC-AUTH-021) o una futura sincronización que no aplica a LOCAL.
 */
@Service
public class CreateLocalUserUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreateLocalUserUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository,
                                   PasswordHasherPort passwordHasher, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public CreateLocalUserResult handle(UUID actorId, CreateLocalUserCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_MANAGE, correlationId);

        if (userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, command.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(); // UC-AUTH-016 flujo 1a, INV-AUTH-001.
        }

        char[] initialPassword = command.initialPassword();
        String generatedPassword = null;
        if (initialPassword == null || initialPassword.length == 0) {
            generatedPassword = OpaqueTokenGenerator.generateSecret(); // alta entropía, cumple la política trivialmente.
            initialPassword = generatedPassword.toCharArray();
        }
        if (!PasswordPolicy.isValid(initialPassword)) {
            throw new PasswordPolicyViolationException();
        }

        Instant now = clock.instant();
        String hash = passwordHasher.hash(initialPassword);
        User user = User.createLocal(UUID.randomUUID(), command.username(), command.displayName(), command.email(),
                PasswordCredential.of(hash, true), now);
        userRepository.save(user);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.USER_CREATED, now,
                actorId, user.id(), correlationId, AuditOutcome.SUCCESS, Map.of("username", command.username())));

        return new CreateLocalUserResult(user, generatedPassword);
    }
}
