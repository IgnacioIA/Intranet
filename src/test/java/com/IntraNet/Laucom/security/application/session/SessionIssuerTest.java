package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.application.authorization.EffectivePermissionsResolver;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre la primera emisión de sesión (login) — UC-AUTH-001 paso 6 / UC-AUTH-002 paso 8. */
@ExtendWith(MockitoExtension.class)
class SessionIssuerTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private EffectivePermissionsResolver permissionsResolver;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private SessionIssuer sessionIssuer;

    @BeforeEach
    void setUp() {
        sessionIssuer = new SessionIssuer(refreshTokenRepository, tokenPort, permissionsResolver, clock, 7);
    }

    @Test
    void issuesANewFamily_andAnAccessTokenWithTheResolvedPermissions() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(UUID.randomUUID(), NOW), NOW);
        when(permissionsResolver.resolve(user)).thenReturn(Set.of("USER_MANAGE"));
        when(tokenPort.issueAccessToken(user, Set.of("USER_MANAGE")))
                .thenReturn(new AccessToken("jwt-value", NOW.plusSeconds(900)));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedSession result = sessionIssuer.issueNewSession(user);

        assertThat(result.accessToken().value()).isEqualTo("jwt-value");
        assertThat(result.refreshTokenSecret()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(NOW.plusSeconds(7L * 24 * 3600));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.userId()).isEqualTo(user.id());
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.expiresAt()).isEqualTo(NOW.plusSeconds(7L * 24 * 3600));
    }
}
