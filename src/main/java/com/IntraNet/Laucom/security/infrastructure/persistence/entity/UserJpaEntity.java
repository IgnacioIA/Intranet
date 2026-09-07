package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Mapeo JPA de {@link com.IntraNet.Laucom.security.domain.model.User}. Anémica a propósito:
 * las invariantes viven en el dominio (ADR-001); esta clase solo transporta el estado hacia/
 * desde la fila de {@code users}.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "email")
    private String email;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "must_change_password_on_next_login", nullable = false)
    private boolean mustChangePasswordOnNextLogin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_ad_sync_at")
    private Instant lastAdSyncAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public UserJpaEntity() {
        // JPA / construcción desde el mapper de persistencia (paquete distinto).
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMustChangePasswordOnNextLogin() {
        return mustChangePasswordOnNextLogin;
    }

    public void setMustChangePasswordOnNextLogin(boolean mustChangePasswordOnNextLogin) {
        this.mustChangePasswordOnNextLogin = mustChangePasswordOnNextLogin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getLastAdSyncAt() {
        return lastAdSyncAt;
    }

    public void setLastAdSyncAt(Instant lastAdSyncAt) {
        this.lastAdSyncAt = lastAdSyncAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
