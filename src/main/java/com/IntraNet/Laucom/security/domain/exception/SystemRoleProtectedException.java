package com.IntraNet.Laucom.security.domain.exception;

/**
 * INV-AUTH-011: un {@link com.IntraNet.Laucom.security.domain.model.Role} de sistema no puede
 * desactivarse ni quedar sin permisos.
 */
public final class SystemRoleProtectedException extends DomainRuleViolationException {

    public SystemRoleProtectedException(String roleName) {
        super("El Role de sistema '" + roleName + "' no puede desactivarse ni quedar sin permisos (INV-AUTH-011)");
    }
}
