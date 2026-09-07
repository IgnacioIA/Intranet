package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta (user_id, role_id) — INV-AUTH-005/INV-AUTH-015: un Role no se duplica por usuario. */
@Embeddable
public class UserRoleAssignmentId implements Serializable {

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    protected UserRoleAssignmentId() {
        // JPA
    }

    public UserRoleAssignmentId(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleAssignmentId other)) return false;
        return Objects.equals(userId, other.userId) && Objects.equals(roleId, other.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
