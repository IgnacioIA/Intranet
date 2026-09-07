package com.IntraNet.Laucom.security.application.exception;

/** UC-AUTH-020, 409 {@code permission-already-exists}. */
public final class PermissionAlreadyExistsException extends ApplicationRuleViolationException {

    public PermissionAlreadyExistsException() {
        super("Ya existe una Permission con ese name");
    }
}
