package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-005 flujo 3b, ADR-008, INV-AUTH-007: se presentó un Refresh Token ya revocado
 * (reutilización). Distinta de {@link InvalidRefreshTokenException} a nivel de aplicación para
 * que la capa API (Fase 19) pueda mapearla a su propio tipo RFC 7807
 * ({@code refresh-token-reused}), aunque ambas terminen en 401 y el cliente reaccione igual
 * (redirigir a login) — SPEC-AUTH-002 §12: "no debe revelar en la respuesta... la distinción es
 * solo interna/de auditoría".
 */
public final class RefreshTokenReuseDetectedException extends ApplicationRuleViolationException {

    public RefreshTokenReuseDetectedException() {
        super("Reutilización de Refresh Token detectada");
    }
}
