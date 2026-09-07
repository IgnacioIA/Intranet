package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-005 SPEC-AUTH-002: el Refresh Token presentado no existe, expiró, o la cuenta ya no
 * está {@code ACTIVE} (RN-03). La capa API (Fase 19) la mapea a 401 + {@code invalid-refresh-token}.
 */
public final class InvalidRefreshTokenException extends ApplicationRuleViolationException {

    public InvalidRefreshTokenException() {
        super("Refresh Token inválido o expirado");
    }
}
