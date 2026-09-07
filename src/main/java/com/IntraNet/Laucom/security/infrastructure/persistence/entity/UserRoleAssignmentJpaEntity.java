package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Mapeo JPA de {@link com.IntraNet.Laucom.security.domain.model.UserRoleAssignment}.
 *
 * <p>No se modela como {@code @OneToMany} bidireccional sobre {@code UserJpaEntity}: al tener
 * clave compuesta y ser un hijo simple del agregado User, el adapter ({@code
 * JpaUserRepositoryAdapter}) lo gestiona explícitamente con un patrón borrar-e-insertar en
 * cada {@code save}, evitando la complejidad de Hibernate para colecciones con clave
 * compuesta (simplicidad antes que complejidad, .claude/core/principles.md).</p>
 */
@Entity
@Table(name = "user_role_assignments")
public class UserRoleAssignmentJpaEntity {

    @EmbeddedId
    private UserRoleAssignmentId id;

    @Column(name = "provenance", length = 30, nullable = false)
    private String provenance;

    @Column(name = "source_ad_group", length = 200)
    private String sourceAdGroup;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRoleAssignmentJpaEntity() {
        // JPA
    }

    public UserRoleAssignmentJpaEntity(UserRoleAssignmentId id, String provenance, String sourceAdGroup, Instant assignedAt) {
        this.id = id;
        this.provenance = provenance;
        this.sourceAdGroup = sourceAdGroup;
        this.assignedAt = assignedAt;
    }

    public UserRoleAssignmentId getId() {
        return id;
    }

    public String getProvenance() {
        return provenance;
    }

    public String getSourceAdGroup() {
        return sourceAdGroup;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
