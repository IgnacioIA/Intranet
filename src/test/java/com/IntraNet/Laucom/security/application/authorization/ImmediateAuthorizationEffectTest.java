package com.IntraNet.Laucom.security.application.authorization;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Fase 7 — Autorización Inmediata. Cierra explícitamente REQ-AUTH-015 y el escenario Gherkin
 * "Cambio de permisos con efecto inmediato" de SPEC-AUTH-006 §8: a diferencia de
 * {@link AuthorizationServiceTest}, que verifica cada regla de forma aislada, esta clase
 * verifica la propiedad de punta a punta — dos llamadas consecutivas a
 * {@link AuthorizationService}, con una mutación de estado real entre medio y <b>sin</b>
 * ningún artefacto de sesión (token nuevo, caché) involucrado — porque {@code AuthorizationService}
 * no cachea nada (ADR-006): cada llamada vuelve a consultar los Ports.
 */
@ExtendWith(MockitoExtension.class)
class ImmediateAuthorizationEffectTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String PERMISSION = "USER_MANAGE";

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
        // auditPort no se stubea: record() es void y Mockito no hace nada por defecto, que es
        // exactamente el comportamiento deseado aquí (esta clase no verifica auditoría).
        authorizationService = new AuthorizationService(userRepository, roleRepository, auditPort, clock);
    }

    @Test
    void revokingAPermissionFromARole_denAccessFromTheVeryNextCall_REQ_AUTH_015() {
        UUID roleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        Permission permission = Permission.create(PERMISSION, "Gestionar usuarios");
        role.grant(permission);

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isTrue();

        role.revoke(permission); // un administrador retira el permiso del rol — sin emitir ningún token nuevo.

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION))
                .as("la siguiente llamada, sin ningún nuevo token ni caché de por medio, debe denegar de inmediato")
                .isFalse();
    }

    @Test
    void deactivatingTheGrantingRole_deniesAccessFromTheVeryNextCall_ADR_020() {
        UUID roleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isTrue();

        role.deactivate();

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isFalse();
    }

    @Test
    void revokingTheUsersOnlyRole_deniesAccessFromTheVeryNextCall() {
        UUID roleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        lenient().when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isTrue();

        user.revokeRole(roleId); // -> PENDING_ONBOARDING (INV-AUTH-004), sin nuevo login de por medio.

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isFalse();
    }

    @Test
    void disablingTheAccount_deniesAccessFromTheVeryNextCall_evenWithThePermissionStillGranted_REQ_AUTH_015() {
        UUID roleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        Role role = Role.create(roleId, "ADMIN", "Administrador");
        role.grant(Permission.create(PERMISSION, "Gestionar usuarios"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        lenient().when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION)).isTrue();

        user.disable(NOW); // equivalente, a nivel de dominio, a la operación administrativa de REQ-AUTH-028.

        assertThat(authorizationService.isAuthorized(user.id(), PERMISSION))
                .as("REQ-AUTH-015: deshabilitar impide el acceso desde el siguiente request, "
                        + "aunque el permiso subyacente del rol siga intacto")
                .isFalse();
    }
}
