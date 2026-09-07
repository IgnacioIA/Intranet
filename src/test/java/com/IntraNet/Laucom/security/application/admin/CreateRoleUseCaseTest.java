package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.application.exception.RoleAlreadyExistsException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-019 (SPEC-AUTH-010) alta de Role. */
@ExtendWith(MockitoExtension.class)
class CreateRoleUseCaseTest {

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
    private CreateRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new CreateRoleUseCase(authorizer, roleRepository, permissionRepository, auditPort, clock);
    }

    @Test
    void createsARole_withTheRequestedPermissions() {
        Permission permission = Permission.create("CONTENT_READ", "Leer");
        when(roleRepository.findByName("CONTENT_EDITOR")).thenReturn(Optional.empty());
        when(permissionRepository.findByName("CONTENT_READ")).thenReturn(Optional.of(permission));

        Role result = useCase.handle(ACTOR_ID,
                new CreateRoleCommand("CONTENT_EDITOR", "Edita contenido", Set.of("CONTENT_READ")), "corr-1");

        assertThat(result.name()).isEqualTo("CONTENT_EDITOR");
        assertThat(result.description()).isEqualTo("Edita contenido");
        assertThat(result.permissions()).extracting(Permission::name).containsExactly("CONTENT_READ");
        verify(roleRepository).save(result);
    }

    @Test
    void createsARole_withoutPermissions_whenNoneAreRequested() {
        when(roleRepository.findByName("EMPTY_ROLE")).thenReturn(Optional.empty());

        Role result = useCase.handle(ACTOR_ID, new CreateRoleCommand("EMPTY_ROLE", "Sin permisos", null), "corr-1");

        assertThat(result.permissions()).isEmpty();
    }

    @Test
    void duplicateName_isRejected() {
        when(roleRepository.findByName("CONTENT_EDITOR"))
                .thenReturn(Optional.of(Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "existente")));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateRoleCommand("CONTENT_EDITOR", "nuevo", Set.of()), "corr-1"))
                .isInstanceOf(RoleAlreadyExistsException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void unknownPermissionName_isRejected() {
        when(roleRepository.findByName("CONTENT_EDITOR")).thenReturn(Optional.empty());
        when(permissionRepository.findByName("DOES_NOT_EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateRoleCommand("CONTENT_EDITOR", "desc", Set.of("DOES_NOT_EXIST")), "corr-1"))
                .isInstanceOf(PermissionNotFoundException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateRoleCommand("CONTENT_EDITOR", "desc", Set.of()), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(roleRepository, never()).findByName(any());
    }
}
