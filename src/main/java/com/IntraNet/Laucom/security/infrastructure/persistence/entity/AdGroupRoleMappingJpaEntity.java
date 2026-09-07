package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Mapeo JPA de {@link com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping}. Ver
 * entities.md "AdGroupRoleMapping", INV-AUTH-010.
 */
@Entity
@Table(name = "ad_group_role_mappings")
public class AdGroupRoleMappingJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "ad_group_identifier", length = 200, nullable = false, unique = true)
    private String adGroupIdentifier;

    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 36, nullable = false)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AdGroupRoleMappingJpaEntity() {
        // JPA
    }

    public AdGroupRoleMappingJpaEntity(String id, String adGroupIdentifier, String roleId, String createdBy,
                                        String updatedBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.adGroupIdentifier = adGroupIdentifier;
        this.roleId = roleId;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getAdGroupIdentifier() {
        return adGroupIdentifier;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdGroupRoleMappingJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
