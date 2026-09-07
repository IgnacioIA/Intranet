package com.IntraNet.Laucom.security.infrastructure.token;

import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.AccessTokenClaims;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter de {@link TokenPort} vía JWT firmado con RSA (ADR-007, ADR-009). RS256 se elige entre
 * las opciones que habilita el ADR (RS256/ES256) por ser la más ampliamente soportada por
 * herramientas de inspección/depuración — decisión de bajo impacto y reversible
 * (`.claude/core/decision-authority.md`, mismo criterio que la política de contraseñas de
 * SPEC-AUTH-007), no requiere un ADR propio.
 *
 * <p>El claim {@code permissions} es puramente informativo (ej. para que un frontend oculte
 * opciones de UI sin una consulta adicional): nunca se usa, ni debe usarse, como fuente de
 * autorización — esa evaluación vive exclusivamente en {@code AuthorizationService} contra la
 * base de datos (ADR-006, INV-AUTH-012).</p>
 */
@Component
public class JwtTokenAdapter implements TokenPort {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenAdapter.class);
    private static final String PERMISSIONS_CLAIM = "permissions";

    private final JwtSigningKeys signingKeys;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtTokenAdapter(JwtSigningKeys signingKeys,
                            @Value("${jwt.issuer}") String issuer,
                            @Value("${jwt.audience}") String audience,
                            @Value("${jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
                            Clock clock) {
        this.signingKeys = signingKeys;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds); // ADR-007: objetivo 15 min (900s).
        this.clock = clock;
    }

    @Override
    public AccessToken issueAccessToken(User user, Set<String> permissionNamesSnapshot) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTokenTtl);
        List<String> sortedPermissions = permissionNamesSnapshot.stream().sorted().toList();

        String compact = Jwts.builder()
                .header().keyId(signingKeys.keyId()).and()
                .subject(user.id().toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim(PERMISSIONS_CLAIM, sortedPermissions)
                .signWith(signingKeys.privateKey(), Jwts.SIG.RS256)
                .compact();

        return new AccessToken(compact, expiresAt);
    }

    @Override
    public Optional<AccessTokenClaims> parse(String tokenValue) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKeys.publicKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    // Sin esto, JJWT valida "exp"/"nbf" contra el reloj real del sistema, no
                    // contra el Clock inyectado — rompería tanto la testabilidad (Clock.fixed)
                    // como, en producción, cualquier necesidad futura de ajustar el reloj.
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(tokenValue);
            Claims claims = jws.getPayload();
            String resolvedAudience = claims.getAudience().stream().findFirst().orElse(null);
            return Optional.of(new AccessTokenClaims(UUID.fromString(claims.getSubject()), claims.getIssuer(),
                    resolvedAudience, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(),
                    claims.getId()));
        } catch (JwtException | IllegalArgumentException e) {
            // Firma inválida, token expirado, issuer/audience incorrectos, subject no es un UUID
            // válido, etc.: todos son, para el llamante, "token no utilizable" (Optional.empty()),
            // tal como define el contrato de TokenPort — no se filtra la causa técnica al dominio.
            log.debug("Access Token no verificable", e);
            return Optional.empty();
        }
    }
}
