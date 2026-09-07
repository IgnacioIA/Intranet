package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.domain.exception.SystemPermissionProtectedException;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-020 (SPEC-AUTH-010), activar/desactivar. */
@ExtendWith(MockitoExtension.class)
class SetPermissionActiveUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private PermissionRepositoryPort permissionRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private SetPermissionActiveUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new SetPermissionActiveUseCase(authorizer, permissionRepository, auditPort, clock);
    }

    @Test
    void deactivatesAnOrdinaryPermission() {
        Permission permission = Permission.create("CONTENT_PUBLISH", "Publicar");
        when(permissionRepository.findByName("CONTENT_PUBLISH")).thenReturn(Optional.of(permission));

        Permission result = useCase.handle(ACTOR_ID, "CONTENT_PUBLISH", false, "corr-1");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void deactivatingASystemPermission_isRejected_RN09() {
        Permission systemPermission = Permission.createSystemPermission(WellKnownPermissions.VIEW_ONBOARDING_INFO, "Onboarding");
        when(permissionRepository.findByName(WellKnownPermissions.VIEW_ONBOARDING_INFO))
                .thenReturn(Optional.of(systemPermission));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, WellKnownPermissions.VIEW_ONBOARDING_INFO, false, "corr-1"))
                .isInstanceOf(SystemPermissionProtectedException.class);
    }
}
