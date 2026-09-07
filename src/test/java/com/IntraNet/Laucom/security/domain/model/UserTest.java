package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.InvalidUserStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre INV-AUTH-004, INV-AUTH-005, INV-AUTH-015 y las transiciones de
 * docs/02-domain/transitions.md. No sustituye a los TEST-AUTH-NNN de la Fase 20.
 */
class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void createLocal_startsInPendingOnboarding_withoutRoles() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", true), NOW);

        assertThat(user.status()).isEqualTo(UserStatus.PENDING_ONBOARDING);
        assertThat(user.roles()).isEmpty();
        assertThat(user.isLocal()).isTrue();
        assertThat(user.canAuthenticate()).isTrue();
    }

    @Test
    void assigningFirstRole_transitionsToActive_INV_AUTH_004() {
        User user = localUser();
        UUID roleId = UUID.randomUUID();

        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.findAssignment(roleId)).isPresent();
    }

    @Test
    void revokingLastRole_returnsToPendingOnboarding_RN15() {
        User user = localUser();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);

        user.revokeRole(roleId);

        assertThat(user.status()).isEqualTo(UserStatus.PENDING_ONBOARDING);
    }

    @Test
    void explicitAssignment_isNeverOverwrittenByDerivedSync_INV_AUTH_005() {
        User user = localUser();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW);

        user.reconcileDerivedRoles(Set.of(), NOW); // sin grupos AD vigentes

        assertThat(user.findAssignment(roleId)).hasValueSatisfying(
                a -> assertThat(a.provenance()).isEqualTo(RoleProvenance.GRANTED_EXPLICITLY));
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE); // el explícito lo sostiene
    }

    @Test
    void derivedAssignment_upgradesToExplicit_withoutDuplicating_INV_AUTH_015() {
        User user = localUser();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.derivedFromAd(roleId, "IT-SUPPORT", NOW), NOW);

        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW.plusSeconds(1)), NOW.plusSeconds(1));

        assertThat(user.roles()).hasSize(1);
        assertThat(user.findAssignment(roleId)).hasValueSatisfying(
                a -> assertThat(a.provenance()).isEqualTo(RoleProvenance.GRANTED_EXPLICITLY));
    }

    @Test
    void reconcileDerivedRoles_removesStaleDerived_keepsExplicit() {
        User user = localUser();
        UUID derivedRole = UUID.randomUUID();
        UUID explicitRole = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.derivedFromAd(derivedRole, "IT-SUPPORT", NOW), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(explicitRole, NOW), NOW);

        user.reconcileDerivedRoles(Set.of(), NOW.plusSeconds(1)); // el grupo ya no está mapeado

        assertThat(user.findAssignment(derivedRole)).isEmpty();
        assertThat(user.findAssignment(explicitRole)).isPresent();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void disable_revokesAuthentication_andRejectsFromTerminalState() {
        User user = localUser();

        user.disable(NOW);

        assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.canAuthenticate()).isFalse();
        assertThatThrownBy(() -> user.disable(NOW)).isInstanceOf(InvalidUserStateTransitionException.class);
    }

    @Test
    void deprovision_isTerminal() {
        User user = localUser();

        user.deprovision(NOW);

        assertThat(user.status()).isEqualTo(UserStatus.DEPROVISIONED);
        assertThatThrownBy(() -> user.enable(NOW)).isInstanceOf(InvalidUserStateTransitionException.class);
        assertThatThrownBy(() -> user.deprovision(NOW)).isInstanceOf(InvalidUserStateTransitionException.class);
    }

    @Test
    void enable_reevaluatesRoles_insteadOfBlindlyActivating() {
        User user = localUser();
        user.disable(NOW);

        user.enable(NOW.plusSeconds(1));

        assertThat(user.status()).isEqualTo(UserStatus.PENDING_ONBOARDING); // sin roles vigentes
    }

    @Test
    void recordFailedLoginAttempt_locksAfterThreshold() {
        User user = localUser();
        UUID roleId = UUID.randomUUID();
        user.assignRole(UserRoleAssignment.grantedExplicitly(roleId, NOW), NOW); // -> ACTIVE

        user.recordFailedLoginAttempt(3, NOW, Duration.ofMinutes(5));
        user.recordFailedLoginAttempt(3, NOW, Duration.ofMinutes(5));
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        user.recordFailedLoginAttempt(3, NOW, Duration.ofMinutes(5));

        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.lockedUntil()).isPresent();
    }

    @Test
    void localUserPasswordChange_rejectedForActiveDirectoryUser() {
        User adUser = User.provisionFromDirectory(UUID.randomUUID(), "guid-1", "jdoe", "Jane Doe",
                null, NOW);

        assertThatThrownBy(() -> adUser.changeLocalPassword(PasswordCredential.of("hash", false)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static User localUser() {
        return User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
    }
}
