package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Mapeo JPA de un intento individual del log de {@code RateLimiterPort} (Fase 13, ADR-013). */
@Entity
@Table(name = "rate_limit_attempts")
public class RateLimitAttemptJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "rate_key", length = 200, nullable = false)
    private String rateKey;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected RateLimitAttemptJpaEntity() {
        // JPA
    }

    public RateLimitAttemptJpaEntity(String id, String rateKey, Instant attemptedAt) {
        this.id = id;
        this.rateKey = rateKey;
        this.attemptedAt = attemptedAt;
    }

    public String getId() {
        return id;
    }

    public String getRateKey() {
        return rateKey;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
