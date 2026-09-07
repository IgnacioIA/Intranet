package com.IntraNet.Laucom.security.domain.exception;

/**
 * RN-09 SPEC-AUTH-010: una {@link com.IntraNet.Laucom.security.domain.model.Permission} de
 * sistema (ej. VIEW_ONBOARDING_INFO) no puede desactivarse.
 */
public final class SystemPermissionProtectedException extends DomainRuleViolationException {

    public SystemPermissionProtectedException(String permissionName) {
        super("La Permission de sistema '" + permissionName + "' no puede desactivarse");
    }
}
