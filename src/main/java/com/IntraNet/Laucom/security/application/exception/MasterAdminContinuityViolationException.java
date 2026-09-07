package com.IntraNet.Laucom.security.application.exception;

/**
 * RN-07 SPEC-AUTH-010, INV-AUTH-013: la operación dejaría al sistema sin ningún {@code User}
 * {@code ACTIVE} con el rol {@code MASTER_ADMIN}. Rechazo transaccional y bloqueante, nunca una
 * alerta posterior. 409 {@code master-admin-continuity-violation}.
 */
public final class MasterAdminContinuityViolationException extends ApplicationRuleViolationException {

    public MasterAdminContinuityViolationException() {
        super("La operación dejaría al sistema sin ningún administrador MASTER_ADMIN activo");
    }
}
