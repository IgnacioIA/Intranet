package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.IdentityManagedExternallyException;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-010 y los escenarios de SPEC-AUTH-007 §8 con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private SessionRevocationPort sessionRevocation;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangePasswordUseCase(userRepository, passwordHasher, sessionRevocation, auditPort, clock);
    }

    @Test
    void changesPassword_whenCurrentPasswordIsCorrect_andRevokesSessions() {
        User user = localUser("oldHash");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("current".toCharArray(), "oldHash")).thenReturn(true);
        when(passwordHasher.hash("newSecurePassword123".toCharArray())).thenReturn("newHash");

        useCase.handle(new ChangePasswordCommand(user.id(), "current".toCharArray(), "newSecurePassword123".toCharArray()), "corr-1");

        assertThat(user.credential()).hasValueSatisfying(c -> assertThat(c.hash()).isEqualTo("newHash"));
        verify(userRepository).save(user);
        verify(sessionRevocation).revokeAllSessions(user.id());
        verify(auditPort).record(any());
    }

    @Test
    void rejects_whenUserIsActiveDirectory() {
        User adUser = User.provisionFromDirectory(UUID.randomUUID(), "guid", "jdoe", "Jane", null, NOW);
        when(userRepository.findById(adUser.id())).thenReturn(Optional.of(adUser));

        assertThatThrownBy(() -> useCase.handle(
                new ChangePasswordCommand(adUser.id(), "x".toCharArray(), "newSecurePassword123".toCharArray()), "corr-1"))
                .isInstanceOf(IdentityManagedExternallyException.class);

        verifyNoInteractions(sessionRevocation, auditPort);
    }

    @Test
    void rejects_whenCurrentPasswordIsIncorrect() {
        User user = localUser("oldHash");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong".toCharArray(), "oldHash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.handle(
                new ChangePasswordCommand(user.id(), "wrong".toCharArray(), "newSecurePassword123".toCharArray()), "corr-1"))
                .isInstanceOf(InvalidCredentialException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionRevocation);
    }

    @Test
    void rejects_whenNewPasswordViolatesPolicy() {
        User user = localUser("oldHash");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("current".toCharArray(), "oldHash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.handle(
                new ChangePasswordCommand(user.id(), "current".toCharArray(), "short".toCharArray()), "corr-1"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionRevocation);
    }

    private static User localUser(String hash) {
        return User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of(hash, false), NOW);
    }
}
