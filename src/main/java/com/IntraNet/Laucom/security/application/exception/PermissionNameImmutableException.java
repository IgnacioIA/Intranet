package com.IntraNet.Laucom.security.application.exception;

/** RN-10 SPEC-AUTH-010: el name de una Permission es inmutable. 400 {@code permission-name-immutable}. */
public final class PermissionNameImmutableException extends ApplicationRuleViolationException {

    public PermissionNameImmutableException() {
        super("El name de una Permission es inmutable una vez creada; solo su description es editable");
    }
}
