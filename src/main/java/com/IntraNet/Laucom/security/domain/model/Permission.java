package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.SystemPermissionProtectedException;

import java.util.Objects;

/**
 * Value Object identificado por {@code name} (convención {@code RECURSO_ACCION}). Ver
 * entities.md "Permission", INV-AUTH-014 (soft deactivation), ADR-020.
 *
 * <p>{@code name} es inmutable una vez creado; solo {@code description} puede editarse
 * (RN-10 SPEC-AUTH-010).</p>
 */
public final class Permission {

    private final String name;
    private String description;
    private final boolean systemPermission;
    private boolean active;

    private Permission(String name, String description, boolean systemPermission, boolean active) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacío");
        }
        this.name = name;
        this.description = description;
        this.systemPermission = systemPermission;
        this.active = active;
    }

    public static Permission create(String name, String description) {
        return new Permission(name, description, false, true);
    }

    public static Permission createSystemPermission(String name, String description) {
        return new Permission(name, description, true, true);
    }

    /** Reconstrucción desde persistencia (Fase 2). No reaplica invariantes de alta. */
    public static Permission reconstitute(String name, String description, boolean systemPermission, boolean active) {
        return new Permission(name, description, systemPermission, active);
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    /** REQ-AUTH-030, RN-09 SPEC-AUTH-010: un permiso de sistema no puede desactivarse. */
    public void deactivate() {
        if (systemPermission) {
            throw new SystemPermissionProtectedException(name);
        }
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean isSystemPermission() {
        return systemPermission;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Permission[" + name + "]";
    }
}
