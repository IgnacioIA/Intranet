package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.EmailSenderPort;
import com.IntraNet.Laucom.security.domain.port.PasswordRecoveryTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-AUTH-011 SPEC-AUTH-007: solicitud de recuperación de contraseña.
 *
 * <p>RN-02: la respuesta observable es siempre la misma exista o no la cuenta — este Use Case
 * nunca lanza una excepción distinguible por "cuenta inexistente"; solo por límite de intentos
 * excedido (que no es información de la cuenta, es una condición de servicio — igual que
 * ADR-017 para AD).</p>
 */
@Service
public class RequestPasswordRecoveryUseCase {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepositoryPort userRepository;
    private final PasswordRecoveryTokenRepositoryPort recoveryTokenRepository;
    private final RateLimiterPort rateLimiter;
    private final EmailSenderPort emailSender;
    private final AuditPort auditPort;
    private final Clock clock;

    public RequestPasswordRecoveryUseCase(UserRepositoryPort userRepository,
                                           PasswordRecoveryTokenRepositoryPort recoveryTokenRepository,
                                           RateLimiterPort rateLimiter, EmailSenderPort emailSender,
                                           AuditPort auditPort, Clock clock) {
        this.userRepository = userRepository;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.rateLimiter = rateLimiter;
        this.emailSender = emailSender;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(RequestPasswordRecoveryCommand command, String correlationId) {
        RateLimiterPort.RateLimitDecision decision =
                rateLimiter.checkAndRecord("password-recovery:" + command.clientIp(), MAX_ATTEMPTS, WINDOW);
        if (!decision.allowed()) {
            throw new RateLimitExceededException();
        }

        Instant now = clock.instant();
        Optional<User> user = userRepository.findByProviderAndEmail(IdentityProvider.LOCAL, command.email());

        user.ifPresent(u -> {
            // RN-06: invalida cualquier token de recuperación pendiente previo.
            recoveryTokenRepository.findUnusedByUserId(u.id())
                    .forEach(pending -> {
                        pending.invalidate(now);
                        recoveryTokenRepository.save(pending);
                    });

            String secret = OpaqueTokenGenerator.generateSecret();
            PasswordRecoveryToken token = PasswordRecoveryToken.issue(
                    UUID.randomUUID(), u.id(), OpaqueTokenGenerator.hash(secret), now, now.plus(TOKEN_TTL));
            recoveryTokenRepository.save(token);

            emailSender.sendPasswordRecoveryEmail(command.email(), secret);
        });

        // Auditado sin registrar si la cuenta existía (evita fuga incluso en auditoría interna).
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.PASSWORD_RECOVERY_REQUESTED,
                now, null, null, correlationId, AuditOutcome.SUCCESS, Map.of()));
    }
}
