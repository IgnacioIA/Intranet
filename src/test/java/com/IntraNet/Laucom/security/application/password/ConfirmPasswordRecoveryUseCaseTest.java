package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.InvalidRecoveryTokenException;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.PasswordRecoveryTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-012 y los escenarios de SPEC-AUTH-007 §8 con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class ConfirmPasswordRecoveryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private PasswordRecoveryTokenRepositoryPort recoveryTokenRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private SessionRevocationPort sessionRevocation;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ConfirmPasswordRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmPasswordRecoveryUseCase(
                recoveryTokenRepository, userRepository, passwordHasher, sessionRevocation, auditPort, clock);
    }

    @Test
    void confirmsRecovery_marksTokenUsed_andRevokesSessions() {
        String secret = "plain-secret";
        String hash = OpaqueTokenGenerator.hash(secret);
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane", "jdoe@example.com",
                PasswordCredential.of("old", false), NOW);
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), user.id(), hash,
                NOW.minusSeconds(10), NOW.plusSeconds(1000));
        when(recoveryTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(passwordHasher.hash("newSecurePassword123".toCharArray())).thenReturn("newHash");

        useCase.handle(new ConfirmPasswordRecoveryCommand(secret, "newSecurePassword123".toCharArray()), "corr-1");

        assertThat(token.usedAt()).isPresent();
        assertThat(user.credential()).hasValueSatisfying(c -> assertThat(c.hash()).isEqualTo("newHash"));
        verify(sessionRevocation).revokeAllSessions(user.id());
        verify(auditPort).record(any());
    }

    @Test
    void rejectsUnknownToken() {
        when(recoveryTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(
                new ConfirmPasswordRecoveryCommand("bogus", "newSecurePassword123".toCharArray()), "corr-1"))
                .isInstanceOf(InvalidRecoveryTokenException.class);
    }

    @Test
    void rejectsExpiredToken_withoutDistinguishingFromOtherInvalidCases() {
        String hash = OpaqueTokenGenerator.hash("secret");
        PasswordRecoveryToken expired = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), hash,
                NOW.minusSeconds(2000), NOW.minusSeconds(1000));
        when(recoveryTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.handle(
                new ConfirmPasswordRecoveryCommand("secret", "newSecurePassword123".toCharArray()), "corr-1"))
                .isInstanceOf(InvalidRecoveryTokenException.class);
    }

    @Test
    void rejectsAlreadyUsedToken() {
        String hash = OpaqueTokenGenerator.hash("secret");
        PasswordRecoveryToken used = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), hash,
                NOW.minusSeconds(100), NOW.plusSeconds(1000));
        used.markUsed(NOW.minusSeconds(50));
        when(recoveryTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> useCase.handle(
                new ConfirmPasswordRecoveryCommand("secret", "newSecurePassword123".toCharArray()), "corr-1"))
                .isInstanceOf(InvalidRecoveryTokenException.class);
    }

    @Test
    void rejectsWeakNewPassword_beforeTouchingUserRepository() {
        String hash = OpaqueTokenGenerator.hash("secret");
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), hash,
                NOW.minusSeconds(10), NOW.plusSeconds(1000));
        when(recoveryTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> useCase.handle(
                new ConfirmPasswordRecoveryCommand("secret", "short".toCharArray()), "corr-1"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        verifyNoInteractions(userRepository, sessionRevocation);
    }
}
