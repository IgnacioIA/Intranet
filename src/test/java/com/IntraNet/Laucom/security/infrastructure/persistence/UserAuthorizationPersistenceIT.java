package com.IntraNet.Laucom.security.infrastructure.persistence;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.RoleProvenance;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.infrastructure.persistence.adapter.JpaPermissionRepositoryAdapter;
import com.IntraNet.Laucom.security.infrastructure.persistence.adapter.JpaRoleRepositoryAdapter;
import com.IntraNet.Laucom.security.infrastructure.persistence.adapter.JpaUserRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 2 (Persistencia) / docs/03-architecture/testing-strategy.md §1: persistencia probada
 * contra MySQL real vía Testcontainers — no se asume que H2 sea equivalente.
 *
 * <p><b>Nota de entorno:</b> requiere Docker disponible para levantar el contenedor MySQL.
 * No se pudo ejecutar en el sandbox de esta sesión (Docker no instalado); queda como el test
 * permanente del proyecto para CI / una máquina con Docker.</p>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaUserRepositoryAdapter.class, JpaRoleRepositoryAdapter.class, JpaPermissionRepositoryAdapter.class})
class UserAuthorizationPersistenceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private JpaUserRepositoryAdapter userRepository;
    @Autowired
    private JpaRoleRepositoryAdapter roleRepository;
    @Autowired
    private JpaPermissionRepositoryAdapter permissionRepository;

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void savingAndReloadingUser_roundTripsIdentityAndRoleAssignments() {
        Permission permission = Permission.create("CONTENT_READ", "Leer contenido");
        permissionRepository.save(permission);
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "Edición de contenido");
        role.grant(permission);
        roleRepository.save(role);

        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("argon2-hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(role.id(), NOW), NOW);
        userRepository.save(user);

        User reloaded = userRepository.findById(user.id()).orElseThrow();

        assertThat(reloaded.username()).isEqualTo("jdoe");
        assertThat(reloaded.status()).isEqualTo(UserStatus.ACTIVE); // INV-AUTH-004: con rol → ACTIVE
        assertThat(reloaded.roles()).extracting(UserRoleAssignment::roleId).containsExactly(role.id());
        assertThat(reloaded.roles()).extracting(UserRoleAssignment::provenance)
                .containsExactly(RoleProvenance.GRANTED_EXPLICITLY);

        Role reloadedRole = roleRepository.findByName("CONTENT_EDITOR").orElseThrow();
        assertThat(reloadedRole.effectivePermissions()).extracting(Permission::name).containsExactly("CONTENT_READ");
    }

    @Test
    void save_replacesRoleAssignments_ratherThanAccumulatingThem() {
        User user = User.createLocal(UUID.randomUUID(), "revoker", "Revoker", null,
                PasswordCredential.of("hash", false), NOW);
        UUID roleId = UUID.randomUUID();
        Role role = Role.create(roleId, "TEMP_ROLE", null);
        roleRepository.save(role);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        userRepository.save(user);

        user.revokeRole(roleId);
        userRepository.save(user);

        User reloaded = userRepository.findById(user.id()).orElseThrow();
        assertThat(reloaded.roles()).isEmpty();
        assertThat(reloaded.status()).isEqualTo(UserStatus.PENDING_ONBOARDING); // RN-15 SPEC-AUTH-010
    }

    @Test
    void uniqueConstraint_rejectsDuplicateUsernameForSameProvider_INV_AUTH_001() {
        userRepository.save(User.createLocal(UUID.randomUUID(), "duplicate", "A", null,
                PasswordCredential.of("hash", false), NOW));

        assertThatThrownBy(() -> userRepository.save(User.createLocal(UUID.randomUUID(), "duplicate", "B", null,
                PasswordCredential.of("hash", false), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueConstraint_rejectsDuplicateExternalId_INV_AUTH_002() {
        userRepository.save(User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "u1", "U1", null, NOW));

        assertThatThrownBy(() -> userRepository.save(
                User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "u2", "U2", null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void localAndActiveDirectoryUsers_mayShareTheSameUsername_INV_AUTH_001() {
        userRepository.save(User.createLocal(UUID.randomUUID(), "jdoe", "Local Jane", null,
                PasswordCredential.of("hash", false), NOW));

        User adUser = User.provisionFromDirectory(UUID.randomUUID(), "guid-shared", "jdoe", "AD Jane", null, NOW);

        assertThat(userRepository.save(adUser)).isNotNull(); // no debe lanzar: unicidad es por (provider, username)
    }

    /**
     * Reproduce contra MySQL/Hibernate real el escenario que causó
     * {@code ClassCastException: Long cannot be cast to Boolean} en el bootstrap del Master
     * Admin: {@code existsActiveUserWithRole} debe devolver {@code true/false} sin lanzar,
     * incluso cuando la implementación subyacente delega en una query {@code COUNT(*)} nativa
     * (ver Javadoc de {@code UserRoleAssignmentJpaRepository#countActiveUsersWithRole}).
     */
    @Test
    void existsActiveUserWithRole_returnsTrue_whenAnActiveUserHoldsTheRole() {
        Role role = Role.create(UUID.randomUUID(), "REPORT_VIEWER", null);
        roleRepository.save(role);
        User user = User.createLocal(UUID.randomUUID(), "viewer1", "Viewer One", null,
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(role.id(), NOW), NOW);
        userRepository.save(user);

        assertThat(userRepository.existsActiveUserWithRole(role.id())).isTrue();
    }

    @Test
    void existsActiveUserWithRole_returnsFalse_whenNoActiveUserHoldsTheRole() {
        Role role = Role.create(UUID.randomUUID(), "UNUSED_ROLE", null);
        roleRepository.save(role);

        assertThat(userRepository.existsActiveUserWithRole(role.id())).isFalse();
    }
}
