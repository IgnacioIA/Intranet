package com.IntraNet.Laucom.security.application.exception;

/**
 * SPEC-AUTH-006: el actor no posee, entre sus roles vigentes, el permiso requerido por la
 * operación (o su cuenta no está {@code ACTIVE}, RN-03). La capa API (Fase 19) la mapea a 403.
 */
public final class InsufficientPermissionException extends ApplicationRuleViolationException {

    public InsufficientPermissionException() {
        super("Permiso insuficiente para la operación solicitada");
    }
}
