package com.IntraNet.Laucom.security.application.password;

import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.EmailSenderPort;
import com.IntraNet.Laucom.security.domain.port.PasswordRecoveryTokenRepositoryPort;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-011 y RN-02/RN-06 de SPEC-AUTH-007 con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class RequestPasswordRecoveryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordRecoveryTokenRepositoryPort recoveryTokenRepository;
    @Mock
    private RateLimiterPort rateLimiter;
    @Mock
    private EmailSenderPort emailSender;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private RequestPasswordRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordRecoveryUseCase(
                userRepository, recoveryTokenRepository, rateLimiter, emailSender, auditPort, clock);
        lenient().when(rateLimiter.checkAndRecord(any(), anyInt(), any()))
                .thenReturn(new RateLimiterPort.RateLimitDecision(true, 4));
    }

    @Test
    void existingAccount_receivesNewToken_andPreviousPendingTokenIsInvalidated() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane", "jdoe@example.com",
                PasswordCredential.of("h", false), NOW);
        when(userRepository.findByProviderAndEmail(IdentityProvider.LOCAL, "jdoe@example.com"))
                .thenReturn(Optional.of(user));
        PasswordRecoveryToken pending = PasswordRecoveryToken.issue(UUID.randomUUID(), user.id(), "oldHash",
                NOW.minusSeconds(10), NOW.plusSeconds(1000));
        when(recoveryTokenRepository.findUnusedByUserId(user.id())).thenReturn(List.of(pending));

        useCase.handle(new RequestPasswordRecoveryCommand("jdoe@example.com", "127.0.0.1"), "corr-1");

        assertThat(pending.usedAt()).isPresent(); // invalidado por la nueva solicitud (RN-06)
        verify(recoveryTokenRepository).save(pending);
        verify(recoveryTokenRepository).save(argThatNewToken(pending));
        verify(emailSender).sendPasswordRecoveryEmail(eq("jdoe@example.com"), any());
        verify(auditPort).record(any());
    }

    @Test
    void nonExistingAccount_stillAudits_uniformly_butSendsNoEmailNorToken() {
        when(userRepository.findByProviderAndEmail(IdentityProvider.LOCAL, "ghost@example.com"))
                .thenReturn(Optional.empty());

        useCase.handle(new RequestPasswordRecoveryCommand("ghost@example.com", "127.0.0.1"), "corr-1");

        verifyNoInteractions(emailSender);
        verify(recoveryTokenRepository, never()).save(any());
        verify(auditPort).record(any()); // mismo evento se audita exista o no la cuenta (RN-02)
    }

    @Test
    void rateLimitExceeded_throwsBeforeTouchingUserRepository() {
        when(rateLimiter.checkAndRecord(any(), anyInt(), any()))
                .thenReturn(new RateLimiterPort.RateLimitDecision(false, 0));

        assertThatThrownBy(() -> useCase.handle(
                new RequestPasswordRecoveryCommand("jdoe@example.com", "1.2.3.4"), "corr-1"))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(userRepository, emailSender, auditPort);
    }

    private static PasswordRecoveryToken argThatNewToken(PasswordRecoveryToken excluded) {
        return org.mockito.ArgumentMatchers.argThat(t -> t != excluded);
    }
}
