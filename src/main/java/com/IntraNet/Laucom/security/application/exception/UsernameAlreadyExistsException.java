package com.IntraNet.Laucom.security.application.exception;

/** UC-AUTH-016 flujo 1a, INV-AUTH-001. 409 {@code username-already-exists}. */
public final class UsernameAlreadyExistsException extends ApplicationRuleViolationException {

    public UsernameAlreadyExistsException() {
        super("Ya existe un usuario LOCAL con ese username");
    }
}
