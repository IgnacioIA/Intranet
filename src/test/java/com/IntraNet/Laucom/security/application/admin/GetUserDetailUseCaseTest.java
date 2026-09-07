package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-015 (SPEC-AUTH-010): `GET /auth/admin/users/{userId}`. */
@ExtendWith(MockitoExtension.class)
class GetUserDetailUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private GetUserDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new GetUserDetailUseCase(authorizer, userRepository);
    }

    @Test
    void returnsTheUser_whenTheyExist() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        User result = useCase.handle(ACTOR_ID, user.id(), "corr-1");

        assertThat(result).isSameAs(user);
    }

    @Test
    void unknownUser_throwsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, userId, "corr-1"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, UUID.randomUUID(), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
