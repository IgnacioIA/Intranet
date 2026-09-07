package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-019 (SPEC-AUTH-010) modificación: diff de permisos. */
@ExtendWith(MockitoExtension.class)
class UpdateRoleUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private PermissionRepositoryPort permissionRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private UpdateRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new UpdateRoleUseCase(authorizer, roleRepository, permissionRepository, auditPort, clock);
    }

    @Test
    void replacesDescription_andComputesThePermissionDiff() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "old description");
        Permission keep = Permission.create("CONTENT_READ", "Leer");
        Permission drop = Permission.create("CONTENT_DELETE", "Borrar");
        role.grant(keep);
        role.grant(drop);
        Permission add = Permission.create("CONTENT_CREATE", "Crear");

        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("CONTENT_READ")).thenReturn(Optional.of(keep));
        when(permissionRepository.findByName("CONTENT_CREATE")).thenReturn(Optional.of(add));

        Role result = useCase.handle(ACTOR_ID,
                new UpdateRoleCommand(role.id(), "new description", Set.of("CONTENT_READ", "CONTENT_CREATE")),
                "corr-1");

        assertThat(result.description()).isEqualTo("new description");
        assertThat(result.permissions()).extracting(Permission::name)
                .containsExactlyInAnyOrder("CONTENT_READ", "CONTENT_CREATE");
    }

    @Test
    void nullPermissionNames_leavesThePermissionSetUntouched() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "old");
        Permission existing = Permission.create("CONTENT_READ", "Leer");
        role.grant(existing);
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        Role result = useCase.handle(ACTOR_ID, new UpdateRoleCommand(role.id(), "new", null), "corr-1");

        assertThat(result.permissions()).containsExactly(existing);
    }
}
