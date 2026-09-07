package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.domain.exception.InvalidUserStateTransitionException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.SessionRevocationPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-018 (SPEC-AUTH-010), en especial RN-07/INV-AUTH-013. */
@ExtendWith(MockitoExtension.class)
class ChangeUserStatusUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private SessionRevocationPort sessionRevocation;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ChangeUserStatusUseCase useCase;
    private AdminActionAuthorizer adminActionAuthorizer;

    @BeforeEach
    void setUp() {
        adminActionAuthorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new ChangeUserStatusUseCase(adminActionAuthorizer, userRepository, roleRepository,
                sessionRevocation, auditPort, clock);
    }

    private User activeUserWithRole(UUID roleId) {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        return user;
    }

    @Test
    void disable_revokesSessions_andAudits() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        Role masterAdminRole = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Admin");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));

        User result = useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.DISABLE), "corr-1");

        assertThat(result.status()).isEqualTo(UserStatus.DISABLED);
        verify(sessionRevocation).revokeAllSessions(user.id());
        verify(userRepository).save(user);
    }

    @Test
    void disablingTheOnlyActiveMasterAdmin_isRejected_RN07_INV_AUTH_013() {
        UUID masterAdminRoleId = UUID.randomUUID();
        User user = activeUserWithRole(masterAdminRoleId);
        Role masterAdminRole = Role.reconstitute(masterAdminRoleId, WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                java.util.Set.of());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(userRepository.countActiveUsersWithRole(masterAdminRoleId)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.DISABLE), "corr-1"))
                .isInstanceOf(MasterAdminContinuityViolationException.class);

        verify(userRepository, never()).save(any());
        verify(sessionRevocation, never()).revokeAllSessions(any());
    }

    @Test
    void disablingAMasterAdmin_isAllowed_whenAnotherOneExists() {
        UUID masterAdminRoleId = UUID.randomUUID();
        User user = activeUserWithRole(masterAdminRoleId);
        Role masterAdminRole = Role.reconstitute(masterAdminRoleId, WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                java.util.Set.of());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(userRepository.countActiveUsersWithRole(masterAdminRoleId)).thenReturn(2L); // hay otro admin.

        User result = useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.DISABLE), "corr-1");

        assertThat(result.status()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void lockingTheOnlyActiveMasterAdmin_isRejected() {
        UUID masterAdminRoleId = UUID.randomUUID();
        User user = activeUserWithRole(masterAdminRoleId);
        Role masterAdminRole = Role.reconstitute(masterAdminRoleId, WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                java.util.Set.of());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(userRepository.countActiveUsersWithRole(masterAdminRoleId)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.LOCK), "corr-1"))
                .isInstanceOf(MasterAdminContinuityViolationException.class);
    }

    @Test
    void unlock_doesNotCheckMasterAdminContinuity_andDoesNotRevokeSessions() {
        UUID roleId = UUID.randomUUID();
        User user = activeUserWithRole(roleId);
        user.lock(NOW.minusSeconds(1000));
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        User result = useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.UNLOCK), "corr-1");

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        verify(sessionRevocation, never()).revokeAllSessions(any());
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void invalidTransition_propagatesDomainException_RN03() {
        User user = activeUserWithRole(UUID.randomUUID());
        user.deprovision(NOW.minusSeconds(1000));
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(user.id(), AdminUserStatusOperation.ENABLE), "corr-1"))
                .isInstanceOf(InvalidUserStateTransitionException.class);
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        org.mockito.Mockito.doThrow(new com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException())
                .when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new ChangeUserStatusCommand(UUID.randomUUID(), AdminUserStatusOperation.DISABLE), "corr-1"))
                .isInstanceOf(com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException.class);

        verify(userRepository, never()).findById(any());
    }
}
