package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.IdentityManagedExternallyException;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-017 (SPEC-AUTH-010), RN-02. */
@ExtendWith(MockitoExtension.class)
class UpdateLocalUserIdentityUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private UpdateLocalUserIdentityUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new UpdateLocalUserIdentityUseCase(authorizer, userRepository, auditPort, clock);
    }

    @Test
    void updatesEmailAndDisplayName_forALocalUser() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane", "old@example.com",
                PasswordCredential.of("hash", false), NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        User result = useCase.handle(ACTOR_ID,
                new UpdateLocalUserIdentityCommand(user.id(), "new@example.com", "Jane Doe"), "corr-1");

        assertThat(result.email()).contains("new@example.com");
        assertThat(result.displayName()).isEqualTo("Jane Doe");
    }

    @Test
    void rejectsUpdating_anActiveDirectoryUser_RN02() {
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "jdoe", "Jane Doe", null, NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateLocalUserIdentityCommand(user.id(), "new@example.com", "Jane"), "corr-1"))
                .isInstanceOf(IdentityManagedExternallyException.class);
    }
}
