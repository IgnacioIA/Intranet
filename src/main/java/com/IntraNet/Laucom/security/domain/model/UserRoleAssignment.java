package com.IntraNet.Laucom.security.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Asociación entre un {@link User} y un {@link Role}, con procedencia. Vive dentro del
 * agregado {@code User} (no referencia userId explícitamente). Ver entities.md
 * "UserRoleAssignment", INV-AUTH-005, INV-AUTH-015.
 */
public record UserRoleAssignment(UUID roleId, RoleProvenance provenance, String sourceAdGroup, Instant assignedAt) {

    public UserRoleAssignment {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(assignedAt, "assignedAt");
        if (provenance == RoleProvenance.GRANTED_EXPLICITLY && sourceAdGroup != null) {
            throw new IllegalArgumentException("sourceAdGroup solo aplica a asignaciones DERIVED_FROM_AD");
        }
    }

    public static UserRoleAssignment derivedFromAd(UUID roleId, String sourceAdGroup, Instant assignedAt) {
        return new UserRoleAssignment(roleId, RoleProvenance.DERIVED_FROM_AD, sourceAdGroup, assignedAt);
    }

    public static UserRoleAssignment grantedExplicitly(UUID roleId, Instant assignedAt) {
        return new UserRoleAssignment(roleId, RoleProvenance.GRANTED_EXPLICITLY, null, assignedAt);
    }

    /** INV-AUTH-015: upgrade de procedencia; conceptualmente la misma fila, nunca duplicada. */
    public UserRoleAssignment upgradeToExplicit(Instant upgradedAt) {
        return new UserRoleAssignment(roleId, RoleProvenance.GRANTED_EXPLICITLY, null, upgradedAt);
    }
}
