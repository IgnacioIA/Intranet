package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.IdentityManagedExternallyException;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.PasswordPolicy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * UC-AUTH-010 SPEC-AUTH-007: cambio de contraseña autenticado.
 */
@Service
public class ChangePasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final SessionRevocationPort sessionRevocation;
    private final AuditPort auditPort;
    private final Clock clock;

    public ChangePasswordUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                  SessionRevocationPort sessionRevocation, AuditPort auditPort, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessionRevocation = sessionRevocation;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(ChangePasswordCommand command, String correlationId) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new NoSuchElementException("Usuario inexistente"));

        if (!user.isLocal()) {
            // RN-01: no se audita como intento de seguridad fallido — es un uso indebido del
            // endpoint documentado, no una credencial incorrecta.
            throw new IdentityManagedExternallyException();
        }

        PasswordCredential credential = user.credential().orElseThrow();
        if (!passwordHasher.matches(command.currentPassword(), credential.hash())) {
            throw new InvalidCredentialException();
        }

        if (!PasswordPolicy.isValid(command.newPassword())) {
            throw new PasswordPolicyViolationException();
        }

        Instant now = clock.instant();
        String newHash = passwordHasher.hash(command.newPassword());
        user.changeLocalPassword(PasswordCredential.of(newHash, false));
        userRepository.save(user);

        // RN-05: revoca todas las sesiones activas ante la hipótesis de contraseña comprometida.
        sessionRevocation.revokeAllSessions(user.id());

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.PASSWORD_CHANGED, now,
                user.id(), user.id(), correlationId, AuditOutcome.SUCCESS, Map.of()));
    }
}
