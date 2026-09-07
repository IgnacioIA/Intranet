package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.PermissionNameImmutableException;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-020 (SPEC-AUTH-010) modificación, RN-10. */
@ExtendWith(MockitoExtension.class)
class UpdatePermissionDescriptionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private PermissionRepositoryPort permissionRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private UpdatePermissionDescriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new UpdatePermissionDescriptionUseCase(authorizer, permissionRepository, auditPort, clock);
    }

    @Test
    void updatesOnlyTheDescription_whenNameIsUnchangedOrAbsent() {
        Permission permission = Permission.create("CONTENT_PUBLISH", "old");
        when(permissionRepository.findByName("CONTENT_PUBLISH")).thenReturn(Optional.of(permission));

        Permission result = useCase.handle(ACTOR_ID,
                new UpdatePermissionDescriptionCommand("CONTENT_PUBLISH", null, "new description"), "corr-1");

        assertThat(result.description()).isEqualTo("new description");
        assertThat(result.name()).isEqualTo("CONTENT_PUBLISH");
    }

    @Test
    void rejectsChangingTheName_RN10() {
        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdatePermissionDescriptionCommand("CONTENT_PUBLISH", "CONTENT_PUBLISH_V2", "new"), "corr-1"))
                .isInstanceOf(PermissionNameImmutableException.class);

        verify(permissionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
