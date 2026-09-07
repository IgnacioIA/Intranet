package com.IntraNet.Laucom.security.application.authorization;

import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cubre SPEC-AUTH-006 (RN-01 a RN-04) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String REQUIRED_PERMISSION = "USER_MANAGE";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(userRepository, roleRepository, auditPort, clock);
    }

    private User activeUserWithRole(UUID roleId) {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                com.IntraNet.Laucom.security.domain.model.PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW); // deja al usuario ACTIVE
        return user;
    }

    @Test
    void isAuthorized_grantsAccess_whenActiveUserHasPermissionThroughARole() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(REQUIRED_PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), REQUIRED_PERMISSION)).isTrue();
    }

    @Test
    void isAuthorized_deniesAccess_whenNoRoleHasThePermission() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        Role role = Role.create(roleId, "VIEWER", "Solo lectura");
        role.grant(Permission.create("VIEW_ONLY", "Ver"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), REQUIRED_PERMISSION)).isFalse();
    }

    @Test
    void isAuthorized_deniesAccess_whenUserIsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(authorizationService.isAuthorized(userId, REQUIRED_PERMISSION)).isFalse();
    }

    @Test
    void isAuthorized_deniesAccess_whenUserIsNotActive_evenWithMatchingPermission_RN03() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        user.disable(NOW); // DISABLED, aunque su rol tenga el permiso
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(REQUIRED_PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThat(authorizationService.isAuthorized(user.id(), REQUIRED_PERMISSION)).isFalse();
    }

    @Test
    void isAuthorized_deniesAccess_whenGrantingRoleIsDeactivated_ADR020() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(REQUIRED_PERMISSION, "Gestionar usuarios"));
        role.deactivate(); // effectivePermissions() queda vacío (ADR-020)

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), REQUIRED_PERMISSION)).isFalse();
    }

    @Test
    void isAuthorized_grantsAccess_whenPermissionComesFromOneOfSeveralRoles() {
        UUID roleWithout = UUID.randomUUID();
        UUID roleWith = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                com.IntraNet.Laucom.security.domain.model.PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleWithout, NOW), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleWith, NOW), NOW);

        Role withoutPermission = Role.create(roleWithout, "VIEWER", "Solo lectura");
        withoutPermission.grant(Permission.create("VIEW_ONLY", "Ver"));
        Role withPermission = Role.create(roleWith, "ADMIN", "Administrador");
        withPermission.grant(Permission.create(REQUIRED_PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        // lenient: User.roles() no garantiza el orden de iteración (Set.copyOf), y anyMatch()
        // corta apenas encuentra el rol con el permiso — según el orden, alguno de los dos
        // findById puede no invocarse nunca sin que eso indique un problema de producción.
        lenient().when(roleRepository.findById(roleWithout)).thenReturn(Optional.of(withoutPermission));
        lenient().when(roleRepository.findById(roleWith)).thenReturn(Optional.of(withPermission));

        assertThat(authorizationService.isAuthorized(user.id(), REQUIRED_PERMISSION)).isTrue();
    }

    @Test
    void requireAuthorized_doesNothing_andNeverAudits_whenAuthorized() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(REQUIRED_PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        authorizationService.requireAuthorized(user.id(), REQUIRED_PERMISSION, "corr-1");

        verifyNoInteractions(auditPort);
    }

    @Test
    void requireAuthorized_throwsAndAuditsDenied_whenNotAuthorized() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireAuthorized(userId, REQUIRED_PERMISSION, "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(auditPort).record(any());
    }
}
