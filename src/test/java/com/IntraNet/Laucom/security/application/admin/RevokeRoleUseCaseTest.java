package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.AssignmentNotExplicitException;
import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-022 (SPEC-AUTH-010): RN-14 (solo GRANTED_EXPLICITLY), RN-07/INV-AUTH-013, RN-15. */
@ExtendWith(MockitoExtension.class)
class RevokeRoleUseCaseTest {

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
    private RevokeRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.empty());
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new RevokeRoleUseCase(authorizer, userRepository, roleRepository, auditPort, clock);
    }

    @Test
    void revokesAnExplicitAssignment_leavingPendingOnboarding_RN15() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        useCase.handle(ACTOR_ID, new RevokeRoleCommand(user.id(), roleId), "corr-1");

        assertThat(user.findAssignment(roleId)).isEmpty();
        assertThat(user.status()).isEqualTo(UserStatus.PENDING_ONBOARDING);
        verify(userRepository).save(user);
    }

    @Test
    void revokingAPurelyDerivedAssignment_isRejected_RN14() {
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "jdoe", "Jane Doe", null, NOW);
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.derivedFromAd(roleId, "IT-SUPPORT", NOW), NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new RevokeRoleCommand(user.id(), roleId), "corr-1"))
                .isInstanceOf(AssignmentNotExplicitException.class);

        assertThat(user.findAssignment(roleId)).isPresent(); // no se tocó.
        verify(userRepository, never()).save(any());
    }

    @Test
    void revokingANonexistentAssignment_isRejected() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new RevokeRoleCommand(user.id(), UUID.randomUUID()), "corr-1"))
                .isInstanceOf(AssignmentNotExplicitException.class);
    }

    @Test
    void revokingTheOnlyActiveMasterAdmins_role_isRejected() {
        UUID masterAdminRoleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "admin", "Admin", "admin@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(masterAdminRoleId, NOW), NOW);
        Role masterAdminRole = Role.reconstitute(masterAdminRoleId, WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                Set.of());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(userRepository.countActiveUsersWithRole(masterAdminRoleId)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, new RevokeRoleCommand(user.id(), masterAdminRoleId), "corr-1"))
                .isInstanceOf(MasterAdminContinuityViolationException.class);

        assertThat(user.findAssignment(masterAdminRoleId)).isPresent();
    }

    @Test
    void revokingMasterAdminRole_isAllowed_whenAnotherAdminExists() {
        UUID masterAdminRoleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "admin", "Admin", "admin@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(masterAdminRoleId, NOW), NOW);
        Role masterAdminRole = Role.reconstitute(masterAdminRoleId, WellKnownRoles.MASTER_ADMIN, "Admin", true, true,
                Set.of());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
        when(userRepository.countActiveUsersWithRole(masterAdminRoleId)).thenReturn(2L);

        useCase.handle(ACTOR_ID, new RevokeRoleCommand(user.id(), masterAdminRoleId), "corr-1");

        assertThat(user.findAssignment(masterAdminRoleId)).isEmpty();
    }
}
