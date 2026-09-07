package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.application.authorization.EffectivePermissionsResolver;
import com.IntraNet.Laucom.security.application.exception.InvalidRefreshTokenException;
import com.IntraNet.Laucom.security.application.exception.RefreshTokenReuseDetectedException;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.service.OpaqueTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-005 (SPEC-AUTH-002) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class RenewSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String SECRET = "the-plain-refresh-secret";
    private static final String HASH = OpaqueTokenGenerator.hash(SECRET);

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private EffectivePermissionsResolver permissionsResolver;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private RenewSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RenewSessionUseCase(
                refreshTokenRepository, userRepository, tokenPort, permissionsResolver, auditPort, clock, 7);
    }

    private static User activeUser(UUID id) {
        User user = User.createLocal(id, "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW.minusSeconds(10000));
        user.assignRole(UserRoleAssignment.grantedExplicitly(UUID.randomUUID(), NOW.minusSeconds(10000)), NOW.minusSeconds(10000));
        return user;
    }

    @Test
    void validToken_rotatesAtomically_andIssuesANewAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        RefreshToken active = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(active));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(permissionsResolver.resolve(user)).thenReturn(Set.of("USER_MANAGE"));
        when(tokenPort.issueAccessToken(user, Set.of("USER_MANAGE")))
                .thenReturn(new AccessToken("new-jwt", NOW.plusSeconds(900)));

        IssuedSession result = useCase.handle(new RenewSessionCommand(SECRET), "corr-1");

        assertThat(result.accessToken().value()).isEqualTo("new-jwt");
        assertThat(result.refreshTokenSecret()).isNotBlank().isNotEqualTo(SECRET);
        assertThat(active.isRevoked()).isTrue(); // INV-AUTH-006: el padre queda revocado...
        assertThat(active.replacedByTokenId()).isPresent(); // ...y enlazado a su reemplazo.
        verify(refreshTokenRepository).saveRotation(eq(active), any(RefreshToken.class));
        verify(auditPort).record(any());
    }

    @Test
    void unknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new RenewSessionCommand(SECRET), "corr-1"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).revokeAllActiveInFamily(any(), any());
    }

    @Test
    void expiredToken_isRejected_withoutRevokingTheFamily() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100_000), NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.handle(new RenewSessionCommand(SECRET), "corr-1"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).revokeAllActiveInFamily(any(), any());
        verify(refreshTokenRepository, never()).saveRotation(any(), any());
    }

    @Test
    void reusedRevokedToken_revokesTheWholeFamily_andThrowsReuseDetected() {
        UUID userId = UUID.randomUUID();
        RefreshToken parent = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(500), NOW.plusSeconds(100_000));
        RefreshToken child = parent.rotate(UUID.randomUUID(), OpaqueTokenGenerator.hash("child-secret"),
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(parent)); // ya revocado por rotate()

        assertThatThrownBy(() -> useCase.handle(new RenewSessionCommand(SECRET), "corr-1"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(refreshTokenRepository).revokeAllActiveInFamily(parent.familyId(), NOW);
        verify(auditPort).record(any());
        assertThat(child.familyId()).isEqualTo(parent.familyId()); // confirma que es la misma familia
    }

    @Test
    void nonActiveUser_isRejected_andRevokesTheFamily_RN03() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        user.disable(NOW.minusSeconds(10));
        RefreshToken active = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(active));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(new RenewSessionCommand(SECRET), "corr-1"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllActiveInFamily(active.familyId(), NOW);
        verify(refreshTokenRepository, never()).saveRotation(any(), any());
    }

    @Test
    void userNoLongerExists_isRejected_andRevokesTheFamily() {
        UUID userId = UUID.randomUUID();
        RefreshToken active = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(active));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new RenewSessionCommand(SECRET), "corr-1"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllActiveInFamily(active.familyId(), NOW);
    }
}
