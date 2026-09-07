package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-020 (SPEC-AUTH-010): `GET /auth/admin/permissions/{name}`. */
@ExtendWith(MockitoExtension.class)
class GetPermissionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private PermissionRepositoryPort permissionRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private GetPermissionUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new GetPermissionUseCase(authorizer, permissionRepository);
    }

    @Test
    void returnsThePermission_whenItExists() {
        Permission permission = Permission.create("CONTENT_READ", "Leer");
        when(permissionRepository.findByName("CONTENT_READ")).thenReturn(Optional.of(permission));

        Permission result = useCase.handle(ACTOR_ID, "CONTENT_READ", "corr-1");

        assertThat(result).isSameAs(permission);
    }

    @Test
    void unknownPermission_throwsPermissionNotFound() {
        when(permissionRepository.findByName("DOES_NOT_EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, "DOES_NOT_EXIST", "corr-1"))
                .isInstanceOf(PermissionNotFoundException.class);
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, "CONTENT_READ", "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
