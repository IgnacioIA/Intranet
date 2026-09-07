package com.IntraNet.Laucom.security.application.identity;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-009 (SPEC-AUTH-005) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;

    private GetCurrentUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentUserUseCase(userRepository, roleRepository);
    }

    @Test
    void activeUser_returnsRealRolesAndPermissions() {
        UUID roleId = UUID.randomUUID();
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);
        Role role = Role.create(roleId, "CONTENT_EDITOR", "Editor de contenido");
        role.grant(Permission.create("CONTENT_READ", "Leer contenido"));
        role.grant(Permission.create("CONTENT_UPDATE", "Editar contenido"));

        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        CurrentUserView view = useCase.handle(user.id()).orElseThrow();

        assertThat(view.id()).isEqualTo(user.id());
        assertThat(view.provider()).isEqualTo(IdentityProvider.LOCAL);
        assertThat(view.username()).isEqualTo("jdoe");
        assertThat(view.email()).isEqualTo("jdoe@example.com");
        assertThat(view.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(view.roles()).containsExactly("CONTENT_EDITOR");
        assertThat(view.permissions()).containsExactlyInAnyOrder("CONTENT_READ", "CONTENT_UPDATE");
    }

    @Test
    void pendingOnboardingUser_returnsTheVirtualOnboardingRoleAndPermission_SPEC_005_section7() {
        User user = User.createLocal(UUID.randomUUID(), "newbie", "New User", null,
                PasswordCredential.of("hash", true), NOW);
        // Sin roles asignados -> permanece PENDING_ONBOARDING.
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        CurrentUserView view = useCase.handle(user.id()).orElseThrow();

        assertThat(view.status()).isEqualTo(UserStatus.PENDING_ONBOARDING);
        assertThat(view.roles()).containsExactly(WellKnownRoles.ONBOARDING_USER);
        assertThat(view.permissions()).containsExactly(WellKnownPermissions.VIEW_ONBOARDING_INFO);
    }

    @Test
    void unknownUser_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(useCase.handle(userId)).isEmpty();
    }

    @Test
    void emailIsNullable_ADGhostWithoutMappedEmail() {
        User user = User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "jdoe", "Jane Doe", null, NOW);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        CurrentUserView view = useCase.handle(user.id()).orElseThrow();

        assertThat(view.email()).isNull();
    }
}
