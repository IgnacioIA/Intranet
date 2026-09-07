package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.application.authorization.EffectivePermissionsResolver;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Emite una sesión nueva (Access Token + primer Refresh Token de una familia nueva) tras un
 * login exitoso — UC-AUTH-001 paso 6, UC-AUTH-002 paso 8 (SPEC-AUTH-001), ADR-007.
 *
 * <p>Distinto de la rotación (UC-AUTH-005, SPEC-AUTH-002): rotar revoca un token existente y
 * emite su reemplazo <b>de la misma familia</b> ({@link RefreshToken#rotate}); esta clase solo
 * cubre la primera emisión de una familia nueva, que es lo único que ocurre en login. Por eso
 * esa lógica vive directamente en {@code RenewSessionUseCase} (Fase 9) y no aquí.</p>
 */
@Service
public class SessionIssuer {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TokenPort tokenPort;
    private final EffectivePermissionsResolver permissionsResolver;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public SessionIssuer(RefreshTokenRepositoryPort refreshTokenRepository, TokenPort tokenPort,
                          EffectivePermissionsResolver permissionsResolver, Clock clock,
                          @Value("${refresh-token.ttl-days:7}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenPort = tokenPort;
        this.permissionsResolver = permissionsResolver;
        this.clock = clock;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays); // ADR-007: hipótesis inicial 7 días.
    }

    public IssuedSession issueNewSession(User user) {
        Instant now = clock.instant();
        String secret = OpaqueTokenGenerator.generateSecret();
        String tokenHash = OpaqueTokenGenerator.hash(secret);

        RefreshToken refreshToken = RefreshToken.issueNewFamily(UUID.randomUUID(), user.id(), tokenHash, now,
                now.plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshToken);

        AccessToken accessToken = tokenPort.issueAccessToken(user, permissionsResolver.resolve(user));
        return new IssuedSession(accessToken, secret, refreshToken.expiresAt());
    }
}
