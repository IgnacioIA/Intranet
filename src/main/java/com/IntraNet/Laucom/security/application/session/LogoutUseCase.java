package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-AUTH-006 SPEC-AUTH-003: cierre de la sesión/dispositivo actual. RN-01: revoca únicamente
 * la familia del Refresh Token presentado, no todas las del usuario (ver {@link LogoutAllUseCase}
 * para eso). Idempotente por diseño (RN, casos límite §7): nunca falla, con o sin token válido.
 *
 * <p>Identidad de la autenticación de este endpoint: SPEC-AUTH-003 §12 deja explícitamente "a
 * definir en implementación" si se exige Access Token vigente o basta el Refresh Token. Se optó
 * por bastar con el Refresh Token (vía cookie) — igual que {@code POST /auth/refresh} — porque
 * el propio Refresh Token ya identifica unívocamente la familia/sesión a cerrar, sin necesitar
 * verificar el Access Token. Documentado también en `docs/06-specifications/SPEC-AUTH-003...md`
 * (no una redefinición de una decisión aprobada: la propia SPEC delega esta elección).</p>
 */
@Service
public class LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public LogoutUseCase(RefreshTokenRepositoryPort refreshTokenRepository, AuditPort auditPort, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(LogoutCommand command, String correlationId) {
        if (command.refreshTokenSecret() == null || command.refreshTokenSecret().isBlank()) {
            return; // Flujo alternativo: ausente → éxito idempotente, sin nada que auditar.
        }

        String tokenHash = OpaqueTokenGenerator.hash(command.refreshTokenSecret());
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByTokenHash(tokenHash);
        if (maybeToken.isEmpty()) {
            return; // Flujo alternativo: token inexistente/inválido → éxito idempotente igual.
        }

        RefreshToken token = maybeToken.get();
        Instant now = clock.instant();
        refreshTokenRepository.revokeAllActiveInFamily(token.familyId(), now); // RN-01; idempotente por sí mismo.

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.LOGOUT, now,
                token.userId(), token.userId(), correlationId, AuditOutcome.SUCCESS, Map.of()));
    }
}
