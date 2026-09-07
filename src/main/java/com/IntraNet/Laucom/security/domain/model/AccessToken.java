package com.IntraNet.Laucom.security.domain.model;

import java.time.Instant;

/**
 * Valor emitido a un {@link User} tras autenticación/renovación exitosa. Ver ADR-007, ADR-009.
 * El contenido criptográfico concreto (JWT firmado) es responsabilidad del adapter detrás de
 * {@link com.IntraNet.Laucom.security.domain.port.TokenPort}; el dominio solo conoce este valor
 * opaco y su expiración.
 */
public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[REDACTED, expiresAt=" + expiresAt + "]";
    }
}
