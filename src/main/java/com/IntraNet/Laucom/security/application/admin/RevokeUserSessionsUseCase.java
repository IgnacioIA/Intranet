package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-008 SPEC-AUTH-004: revocación administrativa de todas las sesiones (familias de
 * Refresh Token) de un usuario objetivo. No invalida Access Tokens ya emitidos — expiran de
 * forma natural dentro de su ventana corta (§10 de la SPEC, decisión ya aprobada).
 *
 * <p>RN de la implementación (Casos límite de la SPEC): si {@code actorId == targetUserId}, no
 * se bloquea ni se redirige — se ejecuta la misma revocación (el resultado es equivalente al
 * autoservicio de {@code LogoutAllUseCase}, pero por la vía administrativa).</p>
 */
@Service
public class RevokeUserSessionsUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public RevokeUserSessionsUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository,
                                      RefreshTokenRepositoryPort refreshTokenRepository, AuditPort auditPort,
                                      Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    /** @return cantidad de familias activas revocadas (0 si el usuario ya no tenía ninguna — idempotente). */
    public int handle(UUID actorId, UUID targetUserId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.SESSION_REVOKE_ANY, correlationId);

        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new UserNotFoundException();
        }

        Instant now = clock.instant();
        int revokedSessions = refreshTokenRepository.revokeAllActiveForUser(targetUserId, now);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.ADMIN_SESSION_REVOCATION,
                now, actorId, targetUserId, correlationId, AuditOutcome.SUCCESS,
                Map.of("revokedSessions", String.valueOf(revokedSessions))));

        return revokedSessions;
    }
}
