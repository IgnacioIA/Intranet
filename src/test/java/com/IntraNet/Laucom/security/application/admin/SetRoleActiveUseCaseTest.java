package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.domain.exception.SystemRoleProtectedException;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-019 (SPEC-AUTH-010), activar/desactivar. */
@ExtendWith(MockitoExtension.class)
class SetRoleActiveUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private SetRoleActiveUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new SetRoleActiveUseCase(authorizer, roleRepository, auditPort, clock);
    }

    @Test
    void deactivatesAnOrdinaryRole() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "Editor");
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        Role result = useCase.handle(ACTOR_ID, role.id(), false, "corr-1");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void deactivatingMasterAdmin_isRejected_unconditionally_INV_AUTH_011() {
        Role masterAdminRole = Role.reconstitute(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                Set.of());
        when(roleRepository.findById(masterAdminRole.id())).thenReturn(Optional.of(masterAdminRole));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, masterAdminRole.id(), false, "corr-1"))
                .isInstanceOf(SystemRoleProtectedException.class);
    }

    @Test
    void reactivatesADeactivatedRole() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "Editor");
        role.deactivate();
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        Role result = useCase.handle(ACTOR_ID, role.id(), true, "corr-1");

        assertThat(result.isActive()).isTrue();
    }
}
