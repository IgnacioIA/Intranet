package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Primer adapter concreto de {@link SessionRevocationPort} (Fase 9) — hasta esta fase, los Use
 * Cases de Fase 3 que ya lo invocaban (cambio/recuperación de contraseña) se verificaban solo
 * con el Port mockeado, sin un bean real de Spring detrás. "Revocar todas las sesiones" se
 * traduce, en términos de este módulo, en revocar todos los {@code RefreshToken} vigentes del
 * usuario — sin ellos no puede renovarse ninguna sesión (SPEC-AUTH-002).
 */
@Component
public class RefreshTokenSessionRevocationAdapter implements SessionRevocationPort {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final Clock clock;

    public RefreshTokenSessionRevocationAdapter(RefreshTokenRepositoryPort refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Override
    public void revokeAllSessions(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, clock.instant());
    }
}
