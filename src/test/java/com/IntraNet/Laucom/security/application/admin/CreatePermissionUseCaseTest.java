package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.PermissionAlreadyExistsException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-020 (SPEC-AUTH-010) alta de Permission. */
@ExtendWith(MockitoExtension.class)
class CreatePermissionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private PermissionRepositoryPort permissionRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private CreatePermissionUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new CreatePermissionUseCase(authorizer, permissionRepository, auditPort, clock);
    }

    @Test
    void createsAPermission() {
        when(permissionRepository.findByName("CONTENT_READ")).thenReturn(Optional.empty());

        Permission result = useCase.handle(ACTOR_ID, new CreatePermissionCommand("CONTENT_READ", "Leer"), "corr-1");

        assertThat(result.name()).isEqualTo("CONTENT_READ");
        assertThat(result.description()).isEqualTo("Leer");
        verify(permissionRepository).save(result);
    }

    @Test
    void duplicateName_isRejected() {
        when(permissionRepository.findByName("CONTENT_READ"))
                .thenReturn(Optional.of(Permission.create("CONTENT_READ", "existente")));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new CreatePermissionCommand("CONTENT_READ", "nueva"), "corr-1"))
                .isInstanceOf(PermissionAlreadyExistsException.class);

        verify(permissionRepository, never()).save(any());
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new CreatePermissionCommand("CONTENT_READ", "desc"), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(permissionRepository, never()).findByName(any());
    }
}
