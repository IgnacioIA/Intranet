package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.SystemRoleProtectedException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cubre INV-AUTH-011, INV-AUTH-014, ADR-020. */
class RoleTest {

    @Test
    void systemRole_cannotBeDeactivated() {
        Role role = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.ONBOARDING_USER, "Onboarding");

        assertThatThrownBy(role::deactivate).isInstanceOf(SystemRoleProtectedException.class);
    }

    @Test
    void ordinaryRole_canBeDeactivated_andGrantsNothingWhileInactive() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "Edición de contenido");
        Permission permission = Permission.create("CONTENT_READ", "Leer contenido");
        role.grant(permission);

        role.deactivate();

        assertThat(role.isActive()).isFalse();
        assertThat(role.effectivePermissions()).isEmpty(); // ADR-020: sin efecto, pero...
        assertThat(role.permissions()).containsExactly(permission); // ...sin perder la configuración.
    }

    @Test
    void systemRole_cannotLoseItsLastPermission() {
        Role role = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.ONBOARDING_USER, "Onboarding");
        Permission permission = Permission.createSystemPermission(WellKnownPermissions.VIEW_ONBOARDING_INFO, "Ver onboarding");
        role.grant(permission);

        assertThatThrownBy(() -> role.revoke(permission)).isInstanceOf(SystemRoleProtectedException.class);
    }

    @Test
    void reconstitute_restoresPermissionsAndFlags() {
        Permission permission = Permission.create("CONTENT_READ", "Leer contenido");
        Role role = Role.reconstitute(UUID.randomUUID(), "CONTENT_EDITOR", "desc", false, false, Set.of(permission));

        assertThat(role.isActive()).isFalse();
        assertThat(role.permissions()).containsExactly(permission);
    }
}
