package com.IntraNet.Laucom.security.application.authentication;

import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.session.SessionIssuer;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-AUTH-001 SPEC-AUTH-001: autenticación de usuarios {@code LOCAL}.
 *
 * <p>Desde la Fase 9, emite la sesión completa (Access Token + Refresh Token, vía
 * {@link SessionIssuer}) en cada login exitoso, cumpliendo el paso 6 del flujo aprobado. El
 * endpoint HTTP {@code POST /auth/login} en sí (con el transporte de cookie `HttpOnly`) sigue
 * diferido a la Fase 10 (Token Transport).</p>
 */
@Service
public class AuthenticateLocalUserUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_COOLDOWN = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final int MAX_ATTEMPTS_PER_IDENTITY = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    /** Contraseña fija usada solo para igualar el costo temporal de Argon2id cuando el usuario
     * no existe (REQ-AUTH-025: tiempos de respuesta equivalentes) — nunca es una contraseña real. */
    private static final char[] TIMING_PARITY_PASSWORD = "timing-parity-placeholder-never-a-real-password".toCharArray();

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;
    private final RateLimiterPort rateLimiter;
    private final AuditPort auditPort;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;
    private final String timingParityHash;

    public AuthenticateLocalUserUseCase(UserRepositoryPort userRepository, RoleRepositoryPort roleRepository,
                                         PasswordHasherPort passwordHasher, RateLimiterPort rateLimiter,
                                         AuditPort auditPort, SessionIssuer sessionIssuer, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.rateLimiter = rateLimiter;
        this.auditPort = auditPort;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
        this.timingParityHash = passwordHasher.hash(TIMING_PARITY_PASSWORD);
    }

    public AuthenticationResult handle(AuthenticateLocalUserCommand command, String correlationId) {
        enforceRateLimit(command, correlationId);

        Instant now = clock.instant();
        Optional<User> maybeUser = userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, command.username());

        // La comparación Argon2id se ejecuta siempre, exista o no el usuario, y sin importar su
        // estado, para que el costo temporal sea equivalente en todas las ramas de rechazo
        // (REQ-AUTH-025). El resultado se ignora cuando no corresponde evaluarlo.
        String hashToCompare = maybeUser.flatMap(User::credential)
                .map(PasswordCredential::hash)
                .orElse(timingParityHash);
        boolean passwordMatches = passwordHasher.matches(command.password(), hashToCompare);

        if (maybeUser.isEmpty()) {
            audit(SecurityEventType.LOGIN_FAILURE, null, correlationId, now, AuditOutcome.FAILURE,
                    Map.of("reason", "user-not-found"));
            throw new InvalidCredentialException(); // RN-06: idéntico al resto de los rechazos.
        }

        User user = maybeUser.get();
        autoUnlockMasterAdminIfCooldownElapsed(user, now, correlationId); // RN-05 SPEC-AUTH-009: nunca un bloqueo permanente.

        if (!user.canAuthenticate()) {
            // RN-01: LOCKED/DISABLED/DEPROVISIONED. No se revela el estado específico (UC-AUTH-001 3a).
            audit(SecurityEventType.LOGIN_FAILURE, user.id(), correlationId, now, AuditOutcome.DENIED,
                    Map.of("reason", "account-not-authenticatable", "status", user.status().name()));
            throw new InvalidCredentialException();
        }

        if (!passwordMatches) {
            user.recordFailedLoginAttempt(MAX_FAILED_ATTEMPTS, now, LOCK_COOLDOWN); // RN-07: puede transicionar a LOCKED.
            userRepository.save(user);
            audit(SecurityEventType.LOGIN_FAILURE, user.id(), correlationId, now, AuditOutcome.FAILURE,
                    Map.of("reason", "invalid-password"));
            throw new InvalidCredentialException();
        }

        user.recordSuccessfulLogin(now);
        userRepository.save(user);
        audit(SecurityEventType.LOGIN_SUCCESS, user.id(), correlationId, now, AuditOutcome.SUCCESS, Map.of());
        return new AuthenticationResult(user, sessionIssuer.issueNewSession(user));
    }

    /**
     * RN-05 SPEC-AUTH-009 (Decision Ledger 2026-09-06): el bloqueo por intentos fallidos nunca es
     * permanente para la cuenta {@code MASTER_ADMIN} — a diferencia del resto de los usuarios
     * {@code LOCAL} (RN-07 SPEC-AUTH-001), que sí requieren desbloqueo administrativo explícito
     * ({@code ChangeUserStatusUseCase.UNLOCK}). Sin efecto si el usuario no está {@code LOCKED},
     * si el cooldown no expiró, o si no posee el Role {@code MASTER_ADMIN}.
     */
    private void autoUnlockMasterAdminIfCooldownElapsed(User user, Instant now, String correlationId) {
        if (user.status() != UserStatus.LOCKED) {
            return;
        }
        Optional<Role> masterAdminRole = roleRepository.findByName(WellKnownRoles.MASTER_ADMIN);
        if (masterAdminRole.isEmpty() || user.findAssignment(masterAdminRole.get().id()).isEmpty()) {
            return;
        }

        UserStatus statusBeforeAutoUnlock = user.status();
        user.autoUnlockIfCooldownElapsed(now);
        if (user.status() != statusBeforeAutoUnlock) {
            userRepository.save(user);
            auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.USER_UNLOCKED, now,
                    user.id(), user.id(), correlationId, AuditOutcome.SUCCESS, Map.of("trigger", "cooldown-auto-unlock")));
        }
    }

    private void enforceRateLimit(AuthenticateLocalUserCommand command, String correlationId) {
        RateLimiterPort.RateLimitDecision byIp =
                rateLimiter.checkAndRecord("login:ip:" + command.clientIp(), MAX_ATTEMPTS_PER_IP, RATE_LIMIT_WINDOW);
        RateLimiterPort.RateLimitDecision byIdentity = rateLimiter.checkAndRecord(
                "login:identity:LOCAL:" + command.username(), MAX_ATTEMPTS_PER_IDENTITY, RATE_LIMIT_WINDOW);

        if (!byIp.allowed() || !byIdentity.allowed()) {
            audit(SecurityEventType.RATE_LIMIT_EXCEEDED, null, correlationId, clock.instant(), AuditOutcome.DENIED, Map.of());
            throw new RateLimitExceededException();
        }
    }

    private void audit(SecurityEventType type, UUID actorId, String correlationId, Instant now,
                        AuditOutcome outcome, Map<String, String> metadata) {
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), type, now, actorId, actorId, correlationId,
                outcome, metadata));
    }
}
