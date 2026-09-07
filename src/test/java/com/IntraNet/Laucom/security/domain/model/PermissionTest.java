package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.SystemPermissionProtectedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cubre RN-09/RN-10 SPEC-AUTH-010, INV-AUTH-014, ADR-020. */
class PermissionTest {

    @Test
    void systemPermission_cannotBeDeactivated() {
        Permission permission = Permission.createSystemPermission(WellKnownPermissions.VIEW_ONBOARDING_INFO, "Ver onboarding");

        assertThatThrownBy(permission::deactivate).isInstanceOf(SystemPermissionProtectedException.class);
    }

    @Test
    void ordinaryPermission_canBeDeactivatedAndReactivated() {
        Permission permission = Permission.create("CONTENT_PUBLISH", "Publicar contenido");

        permission.deactivate();
        assertThat(permission.isActive()).isFalse();

        permission.activate();
        assertThat(permission.isActive()).isTrue();
    }

    @Test
    void equality_isBasedOnName_notDescription() {
        Permission a = Permission.create("CONTENT_PUBLISH", "Descripción A");
        Permission b = Permission.create("CONTENT_PUBLISH", "Descripción B");

        assertThat(a).isEqualTo(b);
    }
}
