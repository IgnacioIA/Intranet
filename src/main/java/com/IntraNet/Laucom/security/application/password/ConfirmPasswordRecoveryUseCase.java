package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.InvalidRecoveryTokenException;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.PasswordRecoveryTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import com.IntraNet.Laucom.security.domain.service.PasswordPolicy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * UC-AUTH-012 SPEC-AUTH-007: confirmación de recuperación de contraseña.
 */
@Service
public class ConfirmPasswordRecoveryUseCase {

    private final PasswordRecoveryTokenRepositoryPort recoveryTokenRepository;
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final SessionRevocationPort sessionRevocation;
    private final AuditPort auditPort;
    private final Clock clock;

    public ConfirmPasswordRecoveryUseCase(PasswordRecoveryTokenRepositoryPort recoveryTokenRepository,
                                           UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                           SessionRevocationPort sessionRevocation, AuditPort auditPort, Clock clock) {
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessionRevocation = sessionRevocation;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(ConfirmPasswordRecoveryCommand command, String correlationId) {
        Instant now = clock.instant();
        String tokenHash = OpaqueTokenGenerator.hash(command.token());

        PasswordRecoveryToken token = recoveryTokenRepository.findByTokenHash(tokenHash)
                .filter(t -> t.isValid(now))
                .orElseThrow(InvalidRecoveryTokenException::new); // no distingue inexistente/expirado/usado (RN)

        if (!PasswordPolicy.isValid(command.newPassword())) {
            throw new PasswordPolicyViolationException();
        }

        User user = userRepository.findById(token.userId())
                .orElseThrow(() -> new NoSuchElementException("Usuario del token inexistente"));

        String newHash = passwordHasher.hash(command.newPassword());
        user.changeLocalPassword(PasswordCredential.of(newHash, false));
        userRepository.save(user);

        token.markUsed(now);
        recoveryTokenRepository.save(token);

        // RN-05: revoca todas las sesiones activas ante la hipótesis de contraseña comprometida.
        sessionRevocation.revokeAllSessions(user.id());

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.PASSWORD_RECOVERY_CONFIRMED,
                now, user.id(), user.id(), correlationId, AuditOutcome.SUCCESS, Map.of()));
    }
}
