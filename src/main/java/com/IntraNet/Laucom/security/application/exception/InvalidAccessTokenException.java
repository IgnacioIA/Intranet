package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-009 SPEC-AUTH-005 §12: el Access Token identifica a un usuario que ya no existe en
 * base de datos. En la práctica no debería ocurrir (los usuarios nunca se eliminan físicamente,
 * INV-AUTH-014 es sobre Role/Permission, y User tampoco se borra), pero se maneja
 * defensivamente igual que un token inválido — 401 {@code invalid-access-token}.
 */
public final class InvalidAccessTokenException extends ApplicationRuleViolationException {

    public InvalidAccessTokenException() {
        super("Access Token inválido");
    }
}
