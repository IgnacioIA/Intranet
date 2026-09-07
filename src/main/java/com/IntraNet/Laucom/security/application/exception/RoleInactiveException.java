package com.IntraNet.Laucom.security.application.exception;

/** UC-AUTH-021 flujo alternativo: no se puede asignar un Role inactivo. 400 {@code role-inactive}. */
public final class RoleInactiveException extends ApplicationRuleViolationException {

    public RoleInactiveException() {
        super("No se puede asignar un Role inactivo");
    }
}
