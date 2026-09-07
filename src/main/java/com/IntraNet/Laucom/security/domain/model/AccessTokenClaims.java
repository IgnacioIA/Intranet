package com.IntraNet.Laucom.security.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de parsear/validar un Access Token. Contiene únicamente lo necesario para
 * identificar la sesión (ADR-007) — nunca roles ni permisos autoritativos (ADR-006,
 * INV-AUTH-012).
 */
public record AccessTokenClaims(UUID userId, String issuer, String audience, Instant issuedAt,
                                 Instant expiresAt, String jwtId) {
}
