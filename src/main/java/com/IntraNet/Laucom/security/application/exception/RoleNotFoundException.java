package com.IntraNet.Laucom.security.application.exception;

/** SPEC-AUTH-010 §12. 404 {@code role-not-found}. */
public final class RoleNotFoundException extends ApplicationRuleViolationException {

    public RoleNotFoundException() {
        super("Role inexistente");
    }
}
