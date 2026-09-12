package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre UC-AUTH-008 (SPEC-AUTH-004). Los Casos 1-5 y 8 del checklist de implementación se
 * verifican aquí a nivel de Use Case; el Caso 6 (un Refresh Token previamente válido deja de
 * poder usarse) depende de la semántica real de {@code revokeAllActiveForUser} contra MySQL —
 * ya cubierta por el contrato del propio Port y sus tests de integración (Testcontainers,
 * no ejecutables en este sandbox); el Caso 7 (Access Tokens no invalidados) está garantizado
 * arquitectónicamente: este Use Case no depende de {@code TokenPort} en absoluto.
 */
@ExtendWith(MockitoExtension.class)
class RevokeUserSessionsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private RevokeUserSessionsUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new RevokeUserSessionsUseCase(authorizer, userRepository, refreshTokenRepository, auditPort, clock);
    }

    private static User aUser(UUID id) {
        return User.createLocal(id, "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
    }

    @Test
    void revokesEveryActiveFamily_Caso1() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.of(aUser(targetId)));
        when(refreshTokenRepository.revokeAllActiveForUser(targetId, NOW)).thenReturn(3);

        int revokedSessions = useCase.handle(ACTOR_ID, targetId, "corr-1");

        assertThat(revokedSessions).isEqualTo(3);
        verify(refreshTokenRepository).revokeAllActiveForUser(targetId, NOW);
    }

    @Test
    void secondExecution_isIdempotent_returnsZero_Caso2() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.of(aUser(targetId)));
        when(refreshTokenRepository.revokeAllActiveForUser(targetId, NOW)).thenReturn(0);

        int revokedSessions = useCase.handle(ACTOR_ID, targetId, "corr-1");

        assertThat(revokedSessions).isZero();
    }

    @Test
    void userWithoutActiveSessions_succeedsWithZero_Caso3() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.of(aUser(targetId)));
        when(refreshTokenRepository.revokeAllActiveForUser(targetId, NOW)).thenReturn(0);

        int revokedSessions = useCase.handle(ACTOR_ID, targetId, "corr-1");

        assertThat(revokedSessions).isZero(); // no lanza: 200, no un error.
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse_Caso4() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, UUID.randomUUID(), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(userRepository, never()).findById(any());
        verify(refreshTokenRepository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void unknownTargetUser_throwsUserNotFound_andDoesNotRevoke_Caso5() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, targetId, "corr-1"))
                .isInstanceOf(UserNotFoundException.class);

        verify(refreshTokenRepository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void audits_ADMIN_SESSION_REVOCATION_withActorAndSubject_Caso8() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.of(aUser(targetId)));
        when(refreshTokenRepository.revokeAllActiveForUser(targetId, NOW)).thenReturn(2);

        useCase.handle(ACTOR_ID, targetId, "corr-1");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        // Dos eventos: AUTHORIZATION_GRANTED (AdminActionAuthorizer, RN-16) + ADMIN_SESSION_REVOCATION.
        verify(auditPort, org.mockito.Mockito.times(2)).record(captor.capture());
        SecurityAuditEvent revocationEvent = captor.getAllValues().stream()
                .filter(e -> e.eventType() == SecurityEventType.ADMIN_SESSION_REVOCATION)
                .findFirst().orElseThrow();
        assertThat(revocationEvent.actorUserId()).contains(ACTOR_ID);
        assertThat(revocationEvent.subjectUserId()).contains(targetId);
        assertThat(revocationEvent.metadata()).containsEntry("revokedSessions", "2");
    }

    @Test
    void selfRevocation_isNotBlocked_executesTheSameRevocation() {
        // Caso límite SPEC-AUTH-004 §7: actor == target no se bloquea ni se redirige.
        when(userRepository.findById(eq(ACTOR_ID))).thenReturn(Optional.of(aUser(ACTOR_ID)));
        when(refreshTokenRepository.revokeAllActiveForUser(ACTOR_ID, NOW)).thenReturn(1);

        int revokedSessions = useCase.handle(ACTOR_ID, ACTOR_ID, "corr-1");

        assertThat(revokedSessions).isEqualTo(1);
    }
}
