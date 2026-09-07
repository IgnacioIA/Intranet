package com.IntraNet.Laucom.security.application.exception;

/** SPEC-AUTH-010 §12. 404 {@code user-not-found}. */
public final class UserNotFoundException extends ApplicationRuleViolationException {

    public UserNotFoundException() {
        super("Usuario inexistente");
    }
}
