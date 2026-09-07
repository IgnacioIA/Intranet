package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.RefreshTokenNotActiveException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ver entities.md "RefreshToken", ADR-007, ADR-008, INV-AUTH-006, INV-AUTH-007.
 *
 * <p>La atomicidad real de "revocar + emitir hijo" (INV-AUTH-006) se garantiza mediante la
 * transacción de persistencia (Fase 2/9): este objeto en memoria modela la operación
 * conjunta, pero no reemplaza esa garantía transaccional.</p>
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedByTokenId;

    private RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId,
                          Instant issuedAt, Instant expiresAt, Instant revokedAt, UUID replacedByTokenId) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    /** Primer token de una nueva familia (login, ADR-007). */
    public static RefreshToken issueNewFamily(UUID id, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(id, userId, tokenHash, UUID.randomUUID(), issuedAt, expiresAt, null, null);
    }

    /** Reconstrucción desde persistencia (Fase 2). */
    public static RefreshToken reconstitute(UUID id, UUID userId, String tokenHash, UUID familyId,
                                             Instant issuedAt, Instant expiresAt, Instant revokedAt, UUID replacedByTokenId) {
        return new RefreshToken(id, userId, tokenHash, familyId, issuedAt, expiresAt, revokedAt, replacedByTokenId);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isActive(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    /**
     * ADR-008: rotación estricta, sin grace period (RN-05 SPEC-AUTH-002). Revoca este token y
     * produce su reemplazo de la misma familia. Lanza {@link RefreshTokenNotActiveException} si
     * el token ya no está activo — la capa de aplicación interpreta eso como reuse y revoca el
     * resto de la familia (INV-AUTH-007), lo cual excede este objeto individual.
     */
    public RefreshToken rotate(UUID newTokenId, String newTokenHash, Instant now, Instant newExpiresAt) {
        if (!isActive(now)) {
            throw new RefreshTokenNotActiveException();
        }
        this.revokedAt = now;
        RefreshToken child = new RefreshToken(newTokenId, userId, newTokenHash, familyId, now, newExpiresAt, null, null);
        this.replacedByTokenId = child.id;
        return child;
    }

    /** Revocación por logout/logout-all/administración/cambio de estado de User (sin reemplazo). */
    public void revoke(Instant now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public Optional<UUID> replacedByTokenId() {
        return Optional.ofNullable(replacedByTokenId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken[REDACTED, id=" + id + ", familyId=" + familyId + "]";
    }
}
