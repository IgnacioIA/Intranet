package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.SystemRoleProtectedException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que agrupa {@link Permission}. Ver entities.md "Role", ADR-005, ADR-020,
 * INV-AUTH-011, INV-AUTH-014.
 */
public final class Role {

    private final UUID id;
    private final String name;
    private String description;
    private final boolean systemRole;
    private boolean active;
    private final Set<Permission> permissions = new LinkedHashSet<>();

    private Role(UUID id, String name, String description, boolean systemRole, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacío");
        }
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.active = active;
    }

    public static Role create(UUID id, String name, String description) {
        return new Role(id, name, description, false, true);
    }

    /** Roles de sistema (ej. ONBOARDING_USER, MASTER_ADMIN) nacen activos y no pueden desactivarse. */
    public static Role createSystemRole(UUID id, String name, String description) {
        return new Role(id, name, description, true, true);
    }

    /** Reconstrucción desde persistencia (Fase 2). No reaplica invariantes de alta. */
    public static Role reconstitute(UUID id, String name, String description, boolean systemRole,
                                     boolean active, Set<Permission> permissions) {
        Role role = new Role(id, name, description, systemRole, active);
        role.permissions.addAll(permissions);
        return role;
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    public void grant(Permission permission) {
        this.permissions.add(Objects.requireNonNull(permission, "permission"));
    }

    /** INV-AUTH-011: un rol de sistema no puede quedar sin permisos. */
    public void revoke(Permission permission) {
        if (systemRole && permissions.size() <= 1 && permissions.contains(permission)) {
            throw new SystemRoleProtectedException(name);
        }
        this.permissions.remove(permission);
    }

    /** REQ-AUTH-029, RN-09 SPEC-AUTH-010: un rol de sistema no puede desactivarse (INV-AUTH-011). */
    public void deactivate() {
        if (systemRole) {
            throw new SystemRoleProtectedException(name);
        }
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** Permisos vigentes: vacío si el Role está inactivo (ADR-020), aunque conserve su configuración. */
    public Set<Permission> effectivePermissions() {
        return active ? Set.copyOf(permissions) : Set.of();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Permission> permissions() {
        return Set.copyOf(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Role[" + name + "]";
    }
}
