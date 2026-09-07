package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.RoleProvenance;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-021 (SPEC-AUTH-010): RN-11 (upgrade), RN-12 (idempotencia), rol inactivo. */
@ExtendWith(MockitoExtension.class)
class AssignRoleUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AssignRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new AssignRoleUseCase(authorizer, userRepository, roleRepository, auditPort, clock);
    }

    private static User userWithoutRoles() {
        return User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
    }

    @Test
    void assignsANewRole_explicitly() {
        User user = userWithoutRoles();
        UUID roleId = UUID.randomUUID();
        Role role = Role.create(roleId, "CONTENT_EDITOR", "Editor");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        UserRoleAssignment assignment = useCase.handle(ACTOR_ID, new AssignRoleCommand(user.id(), roleId), "corr-1");

        assertThat(assignment.provenance()).isEqualTo(RoleProvenance.GRANTED_EXPLICITLY);
        assertThat(user.status()).isEqualTo(com.IntraNet.Laucom.security.domain.model.UserStatus.ACTIVE);
    }

    @Test
    void upgradesADerivedAssignment_toExplicit_RN11() {
        User user = userWithoutRoles();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.derivedFromAd(roleId, "IT-SUPPORT", NOW.minusSeconds(100)), NOW.minusSeconds(100));
        Role role = Role.create(roleId, "CONTENT_EDITOR", "Editor");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        UserRoleAssignment assignment = useCase.handle(ACTOR_ID, new AssignRoleCommand(user.id(), roleId), "corr-1");

        assertThat(assignment.provenance()).isEqualTo(RoleProvenance.GRANTED_EXPLICITLY);
        assertThat(user.roles()).hasSize(1); // sin duplicar la fila (INV-AUTH-015).
    }

    @Test
    void reassigningAnAlreadyExplicitRole_isIdempotent_RN12() {
        User user = userWithoutRoles();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW.minusSeconds(100)), NOW.minusSeconds(100));
        Role role = Role.create(roleId, "CONTENT_EDITOR", "Editor");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        useCase.handle(ACTOR_ID, new AssignRoleCommand(user.id(), roleId), "corr-1");

        assertThat(user.roles()).hasSize(1);
    }

    @Test
    void assigningAnInactiveRole_isRejected() {
        User user = userWithoutRoles();
        UUID roleId = UUID.randomUUID();
        Role role = Role.create(roleId, "CONTENT_EDITOR", "Editor");
        role.deactivate();
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new AssignRoleCommand(user.id(), roleId), "corr-1"))
                .isInstanceOf(RoleInactiveException.class);
    }

    @Test
    void unknownUser_isRejected() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new AssignRoleCommand(userId, UUID.randomUUID()), "corr-1"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void unknownRole_isRejected() {
        User user = userWithoutRoles();
        UUID roleId = UUID.randomUUID();
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new AssignRoleCommand(user.id(), roleId), "corr-1"))
                .isInstanceOf(RoleNotFoundException.class);
    }
}
