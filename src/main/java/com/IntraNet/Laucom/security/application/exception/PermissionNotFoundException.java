package com.IntraNet.Laucom.security.application.exception;

/** SPEC-AUTH-010 §12. 404 {@code permission-not-found}. */
public final class PermissionNotFoundException extends ApplicationRuleViolationException {

    public PermissionNotFoundException() {
        super("Permission inexistente");
    }
}
