package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.application.authorization.EffectivePermissionsResolver;
import com.IntraNet.Laucom.security.application.exception.InvalidRefreshTokenException;
import com.IntraNet.Laucom.security.application.exception.RefreshTokenReuseDetectedException;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-AUTH-005 SPEC-AUTH-002: renovación de sesión mediante Refresh Token, con rotación estricta
 * (ADR-008, RN-05: sin grace period) y detección de reutilización (INV-AUTH-007).
 *
 * <p>No consulta {@code IdentityDirectoryPort} ni recalcula roles derivados de AD (RN-02): usa
 * el estado ya persistido del usuario. La única verificación de estado es
 * {@code User.status == ACTIVE} (RN-03).</p>
 */
@Service
public class RenewSessionUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserRepositoryPort userRepository;
    private final TokenPort tokenPort;
    private final EffectivePermissionsResolver permissionsResolver;
    private final AuditPort auditPort;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RenewSessionUseCase(RefreshTokenRepositoryPort refreshTokenRepository, UserRepositoryPort userRepository,
                                TokenPort tokenPort, EffectivePermissionsResolver permissionsResolver,
                                AuditPort auditPort, Clock clock,
                                @Value("${refresh-token.ttl-days:7}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenPort = tokenPort;
        this.permissionsResolver = permissionsResolver;
        this.auditPort = auditPort;
        this.clock = clock;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    public IssuedSession handle(RenewSessionCommand command, String correlationId) {
        Instant now = clock.instant();
        String presentedHash = OpaqueTokenGenerator.hash(command.refreshTokenSecret());

        RefreshToken presented = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (presented.isExpired(now)) {
            // 3a: expirado — 401, sin revocar nada adicional (ya está fuera de uso por su cuenta).
            throw new InvalidRefreshTokenException();
        }

        if (presented.isRevoked()) {
            // 3b, ADR-008, INV-AUTH-007: reutilización de un token ya usado en una rotación previa.
            refreshTokenRepository.revokeAllActiveInFamily(presented.familyId(), now);
            audit(SecurityEventType.TOKEN_REUSE_DETECTED, presented.userId(), correlationId, now, AuditOutcome.DENIED,
                    Map.of("familyId", presented.familyId().toString()));
            throw new RefreshTokenReuseDetectedException();
        }

        Optional<User> maybeUser = userRepository.findById(presented.userId());
        User user = maybeUser.orElse(null);

        if (user == null || user.status() != UserStatus.ACTIVE) {
            // 3c, RN-03: una cuenta no ACTIVE no debe poder seguir renovando sesión.
            refreshTokenRepository.revokeAllActiveInFamily(presented.familyId(), now);
            audit(SecurityEventType.LOGIN_FAILURE, presented.userId(), correlationId, now, AuditOutcome.DENIED,
                    Map.of("reason", "account-not-active-on-refresh"));
            throw new InvalidRefreshTokenException();
        }

        String newSecret = OpaqueTokenGenerator.generateSecret();
        String newHash = OpaqueTokenGenerator.hash(newSecret);
        // RN-01, INV-AUTH-006: revocar el presentado y emitir su reemplazo se persisten como una
        // única operación atómica (saveRotation), no como dos llamadas a save() independientes.
        RefreshToken rotated = presented.rotate(UUID.randomUUID(), newHash, now, now.plus(refreshTokenTtl));
        refreshTokenRepository.saveRotation(presented, rotated);

        AccessToken accessToken = tokenPort.issueAccessToken(user, permissionsResolver.resolve(user));
        audit(SecurityEventType.TOKEN_REFRESHED, user.id(), correlationId, now, AuditOutcome.SUCCESS, Map.of());

        return new IssuedSession(accessToken, newSecret, rotated.expiresAt());
    }

    private void audit(SecurityEventType type, UUID subjectUserId, String correlationId, Instant now,
                        AuditOutcome outcome, Map<String, String> metadata) {
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), type, now, subjectUserId, subjectUserId,
                correlationId, outcome, metadata));
    }
}
