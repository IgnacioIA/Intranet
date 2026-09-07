package com.IntraNet.Laucom.security.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ver entities.md "AdGroupRoleMapping", ADR-005, INV-AUTH-010, SPEC-AUTH-008.
 */
public final class AdGroupRoleMapping {

    private final UUID id;
    private final String adGroupIdentifier;
    private UUID roleId;
    private final UUID createdBy;
    private UUID updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private AdGroupRoleMapping(UUID id, String adGroupIdentifier, UUID roleId, UUID createdBy,
                                UUID updatedBy, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(adGroupIdentifier, "adGroupIdentifier");
        if (adGroupIdentifier.isBlank()) {
            throw new IllegalArgumentException("adGroupIdentifier no puede estar vacío");
        }
        this.adGroupIdentifier = adGroupIdentifier;
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.updatedBy = updatedBy;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt;
    }

    public static AdGroupRoleMapping create(UUID id, String adGroupIdentifier, UUID roleId, UUID createdBy, Instant now) {
        return new AdGroupRoleMapping(id, adGroupIdentifier, roleId, createdBy, createdBy, now, now);
    }

    public static AdGroupRoleMapping reconstitute(UUID id, String adGroupIdentifier, UUID roleId, UUID createdBy,
                                                   UUID updatedBy, Instant createdAt, Instant updatedAt) {
        return new AdGroupRoleMapping(id, adGroupIdentifier, roleId, createdBy, updatedBy, createdAt, updatedAt);
    }

    /** RN-01/RN-03 SPEC-AUTH-008: toda modificación se audita con autor y fecha (a cargo de la capa de aplicación). */
    public void changeRole(UUID newRoleId, UUID updatedBy, Instant now) {
        this.roleId = Objects.requireNonNull(newRoleId, "newRoleId");
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String adGroupIdentifier() {
        return adGroupIdentifier;
    }

    public UUID roleId() {
        return roleId;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdGroupRoleMapping other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
