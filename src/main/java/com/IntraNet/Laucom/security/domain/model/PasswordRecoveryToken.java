package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.RecoveryTokenInvalidException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ver entities.md "PasswordRecoveryToken", INV-AUTH-008, SPEC-AUTH-007.
 */
public final class PasswordRecoveryToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant usedAt;

    private PasswordRecoveryToken(UUID id, UUID userId, String tokenHash, Instant issuedAt,
                                   Instant expiresAt, Instant usedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.usedAt = usedAt;
    }

    public static PasswordRecoveryToken issue(UUID id, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new PasswordRecoveryToken(id, userId, tokenHash, issuedAt, expiresAt, null);
    }

    public static PasswordRecoveryToken reconstitute(UUID id, UUID userId, String tokenHash, Instant issuedAt,
                                                       Instant expiresAt, Instant usedAt) {
        return new PasswordRecoveryToken(id, userId, tokenHash, issuedAt, expiresAt, usedAt);
    }

    /** INV-AUTH-008: un solo uso. */
    public void markUsed(Instant now) {
        if (usedAt != null) {
            throw new RecoveryTokenInvalidException("ya fue utilizado");
        }
        if (now.isAfter(expiresAt)) {
            throw new RecoveryTokenInvalidException("expirado");
        }
        this.usedAt = now;
    }

    public boolean isValid(Instant now) {
        return usedAt == null && !now.isAfter(expiresAt);
    }

    /**
     * RN-06 SPEC-AUTH-007: invalida este token porque fue superado por uno nuevo — a
     * diferencia de {@link #markUsed}, no falla si ya está expirado (invalidar un token
     * expirado es un no-op válido) ni representa que se haya confirmado una recuperación real.
     * Reutiliza {@code usedAt} como marca general de "ya no disponible" en vez de introducir un
     * estado nuevo (INV-AUTH-008 no distingue "usado" de "invalidado" a efectos de autorización).
     */
    public void invalidate(Instant now) {
        if (usedAt == null) {
            this.usedAt = now;
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

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> usedAt() {
        return Optional.ofNullable(usedAt);
    }

    @Override
    public String toString() {
        return "PasswordRecoveryToken[REDACTED, id=" + id + "]";
    }
}
