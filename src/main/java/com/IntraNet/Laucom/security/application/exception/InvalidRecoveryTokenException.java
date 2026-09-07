package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-012, flujo 2a SPEC-AUTH-007: token inexistente, expirado o ya usado. No debe
 * distinguirse cuál de las tres condiciones ocurrió (mensaje único).
 */
public final class InvalidRecoveryTokenException extends ApplicationRuleViolationException {

    public InvalidRecoveryTokenException() {
        super("Token de recuperación inválido");
    }
}
