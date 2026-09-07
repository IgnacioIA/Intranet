package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-022 flujo alternativo, RN-14 SPEC-AUTH-010: se intentó revocar "explícitamente" una
 * asignación que en realidad es {@code DERIVED_FROM_AD}. Orienta al mecanismo correcto
 * (SPEC-AUTH-008, o esperar la próxima sincronización). 409 {@code assignment-not-explicit}.
 */
public final class AssignmentNotExplicitException extends ApplicationRuleViolationException {

    public AssignmentNotExplicitException() {
        super("La asignación es DERIVED_FROM_AD, no GRANTED_EXPLICITLY: modifique el mapping AD "
                + "(SPEC-AUTH-008) o espere la próxima sincronización, en vez de revocarla aquí");
    }
}
