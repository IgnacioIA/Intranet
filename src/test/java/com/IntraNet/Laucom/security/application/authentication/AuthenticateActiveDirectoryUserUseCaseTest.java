package com.IntraNet.Laucom.security.application.authentication;

import com.IntraNet.Laucom.security.application.exception.DirectoryGroupLookupFailedException;
import com.IntraNet.Laucom.security.application.exception.DirectoryUnavailableException;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.session.IssuedSession;
import com.IntraNet.Laucom.security.application.session.SessionIssuer;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.RoleProvenance;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
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
import java.util.Set;
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

/** Cubre UC-AUTH-002/003/004 (SPEC-AUTH-001) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class AuthenticateActiveDirectoryUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final RateLimiterPort.RateLimitDecision ALLOWED = new RateLimiterPort.RateLimitDecision(true, 10);
    private static final RateLimiterPort.RateLimitDecision DENIED = new RateLimiterPort.RateLimitDecision(false, 0);

    @Mock
    private IdentityDirectoryPort identityDirectory;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private AdGroupRoleMappingRepositoryPort mappingRepository;
    @Mock
    private RateLimiterPort rateLimiter;
    @Mock
    private AuditPort auditPort;
    @Mock
    private SessionIssuer sessionIssuer;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AuthenticateActiveDirectoryUserUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(rateLimiter.checkAndRecord(anyString(), anyInt(), any())).thenReturn(ALLOWED);
        lenient().when(sessionIssuer.issueNewSession(any())).thenReturn(
                new IssuedSession(new AccessToken("jwt-value", NOW.plusSeconds(900)), "refresh-secret",
                        NOW.plusSeconds(604800)));
        useCase = new AuthenticateActiveDirectoryUserUseCase(
                identityDirectory, userRepository, mappingRepository, rateLimiter, auditPort, sessionIssuer, clock);
    }

    private AuthenticateActiveDirectoryUserCommand command() {
        return new AuthenticateActiveDirectoryUserCommand("jdoe", "correct".toCharArray(), "10.0.0.1");
    }

    @Test
    void newAdUser_withMappedGroup_isProvisionedActive() {
        UUID roleId = UUID.randomUUID();
        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.Authenticated("guid-1", "Jane Doe", "jdoe@example.com",
                        Set.of("IT-SUPPORT")));
        when(userRepository.findByExternalId("guid-1")).thenReturn(Optional.empty());
        when(mappingRepository.findByAdGroupIdentifierIn(Set.of("IT-SUPPORT"))).thenReturn(
                Set.of(AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", roleId, UUID.randomUUID(), NOW)));

        AuthenticationResult outcome = useCase.handle(command(), "corr-1");
        User result = outcome.user();

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.externalId()).contains("guid-1");
        assertThat(result.findAssignment(roleId)).hasValueSatisfying(
                a -> assertThat(a.provenance()).isEqualTo(RoleProvenance.DERIVED_FROM_AD));
        assertThat(outcome.session()).isNotNull();
        verify(userRepository).save(result);
        verify(auditPort, org.mockito.Mockito.atLeast(2)).record(any()); // AD_USER_PROVISIONED + LOGIN_SUCCESS
    }

    @Test
    void newAdUser_withoutAnyMappedGroup_isProvisionedPendingOnboarding() {
        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.Authenticated("guid-2", "Jane Doe", "jdoe@example.com",
                        Set.of("UNMAPPED-GROUP")));
        when(userRepository.findByExternalId("guid-2")).thenReturn(Optional.empty());
        when(mappingRepository.findByAdGroupIdentifierIn(Set.of("UNMAPPED-GROUP"))).thenReturn(Set.of());

        User result = useCase.handle(command(), "corr-1").user();

        assertThat(result.status()).isEqualTo(UserStatus.PENDING_ONBOARDING);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void existingAdUser_rolesAreResynced_onEachLogin() {
        UUID staleRole = UUID.randomUUID();
        UUID newRole = UUID.randomUUID();
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-3", "jdoe", "Jane Doe",
                "jdoe@example.com", NOW.minusSeconds(3600));
        user.assignRole(com.IntraNet.Laucom.security.domain.model.UserRoleAssignment
                .derivedFromAd(staleRole, "OLD-GROUP", NOW.minusSeconds(3600)), NOW.minusSeconds(3600));

        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.Authenticated("guid-3", "Jane Doe", "jdoe@example.com",
                        Set.of("NEW-GROUP")));
        when(userRepository.findByExternalId("guid-3")).thenReturn(Optional.of(user));
        when(mappingRepository.findByAdGroupIdentifierIn(Set.of("NEW-GROUP"))).thenReturn(
                Set.of(AdGroupRoleMapping.create(UUID.randomUUID(), "NEW-GROUP", newRole, UUID.randomUUID(), NOW)));

        User result = useCase.handle(command(), "corr-1").user();

        assertThat(result.findAssignment(staleRole)).isEmpty(); // INV-AUTH-005
        assertThat(result.findAssignment(newRole)).isPresent();
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void explicitRole_survivesResync_evenWithoutMatchingGroup() {
        UUID explicitRole = UUID.randomUUID();
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-4", "jdoe", "Jane Doe",
                "jdoe@example.com", NOW.minusSeconds(3600));
        user.assignRole(com.IntraNet.Laucom.security.domain.model.UserRoleAssignment
                .grantedExplicitly(explicitRole, NOW.minusSeconds(3600)), NOW.minusSeconds(3600));

        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.Authenticated("guid-4", "Jane Doe", "jdoe@example.com", Set.of()));
        when(userRepository.findByExternalId("guid-4")).thenReturn(Optional.of(user));

        User result = useCase.handle(command(), "corr-1").user();

        assertThat(result.findAssignment(explicitRole)).hasValueSatisfying(
                a -> assertThat(a.provenance()).isEqualTo(RoleProvenance.GRANTED_EXPLICITLY));
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void credentialsRejected_throwsGenericInvalidCredential() {
        when(identityDirectory.authenticate(any(), any())).thenReturn(new IdentityDirectoryPort.CredentialsRejected());

        assertThatThrownBy(() -> useCase.handle(command(), "corr-1")).isInstanceOf(InvalidCredentialException.class);

        verify(userRepository, never()).findByExternalId(any());
    }

    @Test
    void directoryUnavailable_throwsDistinguishableException() {
        when(identityDirectory.authenticate(any(), any())).thenReturn(new IdentityDirectoryPort.DirectoryUnavailable());

        assertThatThrownBy(() -> useCase.handle(command(), "corr-1")).isInstanceOf(DirectoryUnavailableException.class);
    }

    @Test
    void groupLookupFailed_failsClosed_withoutSavingUser() {
        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.GroupLookupFailed("guid-5"));

        assertThatThrownBy(() -> useCase.handle(command(), "corr-1"))
                .isInstanceOf(DirectoryGroupLookupFailedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void disabledExistingUser_isRejected_afterSync() {
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-6", "jdoe", "Jane Doe",
                "jdoe@example.com", NOW.minusSeconds(3600));
        user.disable(NOW.minusSeconds(1000));
        when(identityDirectory.authenticate(any(), any())).thenReturn(
                new IdentityDirectoryPort.Authenticated("guid-6", "Jane Doe", "jdoe@example.com", Set.of()));
        when(userRepository.findByExternalId("guid-6")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(command(), "corr-1")).isInstanceOf(InvalidCredentialException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rateLimitExceeded_rejectsBeforeCallingDirectory() {
        when(rateLimiter.checkAndRecord("login:ip:10.0.0.1", 20, java.time.Duration.ofMinutes(15))).thenReturn(DENIED);

        assertThatThrownBy(() -> useCase.handle(command(), "corr-1")).isInstanceOf(RateLimitExceededException.class);

        verify(identityDirectory, never()).authenticate(any(), any());
    }
}
