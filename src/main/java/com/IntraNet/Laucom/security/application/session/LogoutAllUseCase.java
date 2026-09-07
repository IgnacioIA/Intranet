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
 * UC-AUTH-007 SPEC-AUTH-003: cierre de todas las sesiones del usuario (RN-02), en cualquier
 * dispositivo. El Refresh Token presentado solo sirve para identificar de qué usuario se trata
 * (mismo criterio de identidad que {@link LogoutUseCase} — ver su Javadoc); una vez identificado,
 * la revocación alcanza a todas sus familias, incluida la del propio token presentado.
 */
@Service
public class LogoutAllUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public LogoutAllUseCase(RefreshTokenRepositoryPort refreshTokenRepository, AuditPort auditPort, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(LogoutAllCommand command, String correlationId) {
        if (command.refreshTokenSecret() == null || command.refreshTokenSecret().isBlank()) {
            return; // Idempotente: sin token identificable, no hay nada que revocar.
        }

        String tokenHash = OpaqueTokenGenerator.hash(command.refreshTokenSecret());
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByTokenHash(tokenHash);
        if (maybeToken.isEmpty()) {
            return;
        }

        RefreshToken token = maybeToken.get();
        Instant now = clock.instant();
        refreshTokenRepository.revokeAllActiveForUser(token.userId(), now); // RN-02.

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.LOGOUT_ALL, now,
                token.userId(), token.userId(), correlationId, AuditOutcome.SUCCESS, Map.of()));
    }
}
