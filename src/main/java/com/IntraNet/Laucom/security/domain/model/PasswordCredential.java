package com.IntraNet.Laucom.security.domain.model;

import java.util.Objects;

/**
 * Value Object que encapsula el hash Argon2id de una contraseña {@code LOCAL}. Ver
 * entities.md "PasswordCredential", ADR-011. Nunca expone ni serializa el valor original
 * (INV-AUTH-009, REQ-AUTH-023).
 */
public record PasswordCredential(String hash, boolean mustChangeOnNextLogin) {

    public PasswordCredential {
        Objects.requireNonNull(hash, "hash");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("hash no puede estar vacío");
        }
    }

    public static PasswordCredential of(String hash, boolean mustChangeOnNextLogin) {
        return new PasswordCredential(hash, mustChangeOnNextLogin);
    }

    public PasswordCredential withMustChangeOnNextLogin(boolean value) {
        return new PasswordCredential(hash, value);
    }

    @Override
    public String toString() {
        return "PasswordCredential[REDACTED]";
    }
}
