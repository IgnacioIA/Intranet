package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapeo JPA de {@link com.IntraNet.Laucom.security.domain.model.Permission}. Vive en
 * infrastructure — el dominio no conoce esta clase (ADR-001, REQ-AUTH-021).
 */
@Entity
@Table(name = "permissions")
public class PermissionJpaEntity {

    @Id
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_system_permission", nullable = false)
    private boolean systemPermission;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected PermissionJpaEntity() {
        // JPA
    }

    public PermissionJpaEntity(String name, String description, boolean systemPermission, boolean active) {
        this.name = name;
        this.description = description;
        this.systemPermission = systemPermission;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemPermission() {
        return systemPermission;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionJpaEntity other)) return false;
        return name != null && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
