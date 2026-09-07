package com.IntraNet.Laucom.security.infrastructure.token;

import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.AccessTokenClaims;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre ADR-007/ADR-009 con un par de claves RSA generado en memoria solo para la prueba
 * (nunca un secreto real ni versionado). No requiere Spring: instancia el adapter directamente.
 */
class JwtTokenAdapterTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String ISSUER = "laucom-auth-test";
    private static final String AUDIENCE = "laucom-clients-test";

    private JwtSigningKeys signingKeys;
    private JwtTokenAdapter adapter;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        signingKeys = newSigningKeys("kid-1");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        adapter = new JwtTokenAdapter(signingKeys, ISSUER, AUDIENCE, 900, clock);
    }

    private static JwtSigningKeys newSigningKeys(String kid) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return new JwtSigningKeys(privateKeyBase64, publicKeyBase64, kid);
    }

    private static User aUser() {
        return User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
    }

    @Test
    void issuesAToken_thatParsesBackToTheSameClaims() {
        User user = aUser();

        AccessToken token = adapter.issueAccessToken(user, Set.of("USER_MANAGE", "ROLE_MANAGE"));

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(900)); // ADR-007: 15 minutos.

        Optional<AccessTokenClaims> parsed = adapter.parse(token.value());

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(user.id());
        assertThat(parsed.get().issuer()).isEqualTo(ISSUER);
        assertThat(parsed.get().audience()).isEqualTo(AUDIENCE);
        assertThat(parsed.get().issuedAt()).isEqualTo(NOW);
        assertThat(parsed.get().expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(parsed.get().jwtId()).isNotBlank();
    }

    @Test
    void rejectsATokenSignedWithADifferentKey() throws NoSuchAlgorithmException {
        JwtSigningKeys otherKeys = newSigningKeys("kid-2");
        JwtTokenAdapter adapterWithOtherKeys =
                new JwtTokenAdapter(otherKeys, ISSUER, AUDIENCE, 900, Clock.fixed(NOW, ZoneOffset.UTC));

        AccessToken tokenFromAnotherKey = adapterWithOtherKeys.issueAccessToken(aUser(), Set.of());

        assertThat(adapter.parse(tokenFromAnotherKey.value())).isEmpty();
    }

    @Test
    void rejectsAnExpiredToken() {
        Clock past = Clock.fixed(NOW.minusSeconds(1000), ZoneOffset.UTC);
        JwtTokenAdapter expiredIssuerAdapter = new JwtTokenAdapter(signingKeys, ISSUER, AUDIENCE, 1, past);

        AccessToken expiredToken = expiredIssuerAdapter.issueAccessToken(aUser(), Set.of());

        assertThat(adapter.parse(expiredToken.value())).isEmpty();
    }

    @Test
    void rejectsATokenWithAnUnexpectedIssuer() {
        JwtTokenAdapter adapterWithDifferentIssuer =
                new JwtTokenAdapter(signingKeys, "another-issuer", AUDIENCE, 900, Clock.fixed(NOW, ZoneOffset.UTC));

        AccessToken token = adapterWithDifferentIssuer.issueAccessToken(aUser(), Set.of());

        assertThat(adapter.parse(token.value())).isEmpty();
    }

    @Test
    void rejectsATokenWithAnUnexpectedAudience() {
        JwtTokenAdapter adapterWithDifferentAudience =
                new JwtTokenAdapter(signingKeys, ISSUER, "another-audience", 900, Clock.fixed(NOW, ZoneOffset.UTC));

        AccessToken token = adapterWithDifferentAudience.issueAccessToken(aUser(), Set.of());

        assertThat(adapter.parse(token.value())).isEmpty();
    }

    @Test
    void rejectsAMalformedToken() {
        assertThat(adapter.parse("not-a-jwt")).isEmpty();
    }
}
