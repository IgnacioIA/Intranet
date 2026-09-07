package com.IntraNet.Laucom.security.application.session;

/**
 * UC-AUTH-006 SPEC-AUTH-003. {@code refreshTokenSecret} puede ser {@code null}/vacío —
 * flujo alternativo explícito: "Refresh Token ya inválido/ausente → la operación se considera
 * igualmente exitosa" (idempotente).
 */
public record LogoutCommand(String refreshTokenSecret) {
}
