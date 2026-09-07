package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-006 (SPEC-AUTH-003): idempotente por diseño. */
@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String SECRET = "the-plain-refresh-secret";
    private static final String HASH = OpaqueTokenGenerator.hash(SECRET);

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(refreshTokenRepository, auditPort, clock);
    }

    @Test
    void revokesOnlyTheFamilyOfThePresentedToken_RN01() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(token));

        useCase.handle(new LogoutCommand(SECRET), "corr-1");

        verify(refreshTokenRepository).revokeAllActiveInFamily(token.familyId(), NOW);
        verify(auditPort).record(any());
    }

    @Test
    void missingToken_isIdempotentlySuccessful_withoutAuditing() {
        useCase.handle(new LogoutCommand(null), "corr-1");
        useCase.handle(new LogoutCommand(""), "corr-1");
        useCase.handle(new LogoutCommand("  "), "corr-1");

        verify(refreshTokenRepository, never()).revokeAllActiveInFamily(any(), any());
        verify(auditPort, never()).record(any());
    }

    @Test
    void unknownToken_isIdempotentlySuccessful_withoutAuditing() {
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

        useCase.handle(new LogoutCommand(SECRET), "corr-1");

        verify(refreshTokenRepository, never()).revokeAllActiveInFamily(any(), any());
        verify(auditPort, never()).record(any());
    }

    @Test
    void alreadyRevokedToken_isStillSuccessful_calledAgainWithoutError() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.issueNewFamily(UUID.randomUUID(), userId, HASH,
                NOW.minusSeconds(100), NOW.plusSeconds(100_000));
        token.revoke(NOW.minusSeconds(50));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(token));

        useCase.handle(new LogoutCommand(SECRET), "corr-1"); // no debe lanzar (idempotencia, §7).

        verify(refreshTokenRepository).revokeAllActiveInFamily(token.familyId(), NOW);
    }
}
