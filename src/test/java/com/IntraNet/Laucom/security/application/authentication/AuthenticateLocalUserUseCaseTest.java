package com.IntraNet.Laucom.security.application.authentication;

import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.session.IssuedSession;
import com.IntraNet.Laucom.security.application.session.SessionIssuer;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-001 (SPEC-AUTH-001) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class AuthenticateLocalUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final RateLimiterPort.RateLimitDecision ALLOWED = new RateLimiterPort.RateLimitDecision(true, 10);
    private static final RateLimiterPort.RateLimitDecision DENIED = new RateLimiterPort.RateLimitDecision(false, 0);

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private RateLimiterPort rateLimiter;
    @Mock
    private AuditPort auditPort;
    @Mock
    private SessionIssuer sessionIssuer;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AuthenticateLocalUserUseCase useCase;

    @BeforeEach
    void setUp() {
        // El hash de paridad temporal (REQ-AUTH-025) se calcula en el constructor.
        lenient().when(passwordHasher.hash(any())).thenReturn("dummy-hash");
        lenient().when(rateLimiter.checkAndRecord(anyString(), anyInt(), any())).thenReturn(ALLOWED);
        useCase = new AuthenticateLocalUserUseCase(
                userRepository, roleRepository, passwordHasher, rateLimiter, auditPort, sessionIssuer, clock);
    }

    private User activeUser() {
        // createLocal() deja al usuario en PENDING_ONBOARDING (INV-AUTH-004) hasta que tenga al
        // menos un Role; se asigna uno para reflejar un usuario ACTIVE real en estos escenarios.
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("real-hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(UUID.randomUUID(), NOW), NOW);
        return user;
    }

    @Test
    void successfulLogin_recordsSuccess_andAudits_andIssuesASession() {
        User user = activeUser();
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("correct".toCharArray(), "real-hash")).thenReturn(true);
        IssuedSession session = new IssuedSession(new AccessToken("jwt-value", NOW.plusSeconds(900)),
                "refresh-secret", NOW.plusSeconds(604800));
        when(sessionIssuer.issueNewSession(user)).thenReturn(session);

        AuthenticationResult result = useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1");

        assertThat(result.user()).isSameAs(user);
        assertThat(result.session()).isSameAs(session);
        assertThat(user.failedLoginAttempts()).isZero();
        assertThat(user.lastLoginAt()).contains(NOW);
        verify(userRepository).save(user);
        verify(auditPort).record(any());
    }

    @Test
    void wrongPassword_recordsFailedAttempt_andThrowsGenericError() {
        User user = activeUser();
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong".toCharArray(), "real-hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "wrong".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        assertThat(user.failedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void nonexistentUser_comparesAgainstDummyHash_andThrowsSameGenericError() {
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("ghost", "whatever".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        // REQ-AUTH-025: se invoca matches() igual, contra el hash de paridad, nunca se omite la comparación.
        verify(passwordHasher).matches("whatever".toCharArray(), "dummy-hash");
        verify(userRepository, never()).save(any());
    }

    @Test
    void disabledUser_isRejected_withoutRevealingStatus() {
        User user = activeUser();
        user.disable(NOW);
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void lockedUser_isRejected() {
        User user = activeUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLoginAttempt(5, NOW, java.time.Duration.ofMinutes(15));
        }
        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void lockedMasterAdmin_withElapsedCooldown_isAutoUnlocked_andCanLoginAgain_RN05() {
        Role masterAdminRole = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Admin");
        User user = User.createLocal(UUID.randomUUID(), "admin", "Master Admin", "admin@example.com",
                PasswordCredential.of("real-hash", false), NOW.minusSeconds(100_000));
        user.assignRole(UserRoleAssignment.grantedExplicitly(masterAdminRole.id(), NOW.minusSeconds(100_000)),
                NOW.minusSeconds(100_000));
        for (int i = 0; i < 5; i++) {
            user.recordFailedLoginAttempt(5, NOW.minusSeconds(1000), java.time.Duration.ofMinutes(15));
        }
        assertThat(user.status()).isEqualTo(UserStatus.LOCKED); // cooldown fijado en NOW.minusSeconds(1000) + 15min,
        // ya expiró respecto de NOW (el "ahora" que usará el Use Case).
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "admin")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(passwordHasher.matches("correct".toCharArray(), "real-hash")).thenReturn(true);
        IssuedSession session = new IssuedSession(new AccessToken("jwt-value", NOW.plusSeconds(900)),
                "refresh-secret", NOW.plusSeconds(604800));
        when(sessionIssuer.issueNewSession(user)).thenReturn(session);

        AuthenticationResult result = useCase.handle(
                new AuthenticateLocalUserCommand("admin", "correct".toCharArray(), "10.0.0.1"), "corr-1");

        assertThat(result.user()).isSameAs(user);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void lockedMasterAdmin_withCooldownStillActive_remainsRejected() {
        Role masterAdminRole = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Admin");
        User user = User.createLocal(UUID.randomUUID(), "admin", "Master Admin", "admin@example.com",
                PasswordCredential.of("real-hash", false), NOW.minusSeconds(100_000));
        user.assignRole(UserRoleAssignment.grantedExplicitly(masterAdminRole.id(), NOW.minusSeconds(100_000)),
                NOW.minusSeconds(100_000));
        for (int i = 0; i < 5; i++) {
            user.recordFailedLoginAttempt(5, NOW, java.time.Duration.ofMinutes(15)); // cooldown recién fijado: NOW+15min.
        }
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "admin")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("admin", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        assertThat(user.status()).isEqualTo(UserStatus.LOCKED); // el cooldown todavía no expiró: sigue bloqueado.
    }

    @Test
    void lockedOrdinaryUser_isNeverAutoUnlocked_evenWithAnElapsedCooldown() {
        User user = activeUser(); // sin Role MASTER_ADMIN.
        for (int i = 0; i < 5; i++) {
            user.recordFailedLoginAttempt(5, NOW.minusSeconds(1000), java.time.Duration.ofMinutes(15));
        }
        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN))
                .thenReturn(Optional.of(Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Admin")));

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        // RN-07 SPEC-AUTH-001 (no RN-05, que es exclusiva de MASTER_ADMIN): sigue LOCKED pese al
        // cooldown expirado, porque este usuario no posee ese Role.
        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void deprovisionedUser_isRejected() {
        User user = activeUser();
        user.deprovision(NOW);
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void rateLimitExceededByIp_rejectsBeforeLookingUpUser() {
        when(rateLimiter.checkAndRecord("login:ip:10.0.0.1", 20, java.time.Duration.ofMinutes(15))).thenReturn(DENIED);

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(userRepository, never()).findByProviderAndUsername(any(), any());
    }

    @Test
    void rateLimitExceededByIdentity_rejectsBeforeLookingUpUser() {
        when(rateLimiter.checkAndRecord("login:identity:LOCAL:jdoe", 5, java.time.Duration.ofMinutes(15))).thenReturn(DENIED);

        assertThatThrownBy(() -> useCase.handle(
                new AuthenticateLocalUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1"), "corr-1"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(userRepository, never()).findByProviderAndUsername(any(), any());
    }
}
