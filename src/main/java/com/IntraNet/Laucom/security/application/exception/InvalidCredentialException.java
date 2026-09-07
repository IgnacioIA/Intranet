package com.IntraNet.Laucom.security.application.exception;

/**
 * Credencial actual incorrecta (UC-AUTH-010) o, en general, cualquier rechazo de credenciales
 * que deba comunicarse con un mensaje genérico (REQ-AUTH-025: no enumeración).
 */
public final class InvalidCredentialException extends ApplicationRuleViolationException {

    public InvalidCredentialException() {
        super("Credencial inválida");
    }
}
