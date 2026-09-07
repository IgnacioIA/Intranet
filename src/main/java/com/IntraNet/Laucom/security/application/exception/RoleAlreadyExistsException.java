package com.IntraNet.Laucom.security.application.exception;

/** UC-AUTH-019, 409 {@code role-already-exists}. */
public final class RoleAlreadyExistsException extends ApplicationRuleViolationException {

    public RoleAlreadyExistsException() {
        super("Ya existe un Role con ese name");
    }
}
