package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-002 flujo 3a / UC-AUTH-004: credenciales AD correctas pero falla la obtención o
 * evaluación de los grupos del usuario. Fail closed (RN-09 SPEC-AUTH-001, ADR-017): no se
 * emiten tokens. Distinguible intencionalmente de credencial incorrecta. La capa API (Fase 19)
 * la mapea a 503 + {@code ad-sync-failed}.
 */
public final class DirectoryGroupLookupFailedException extends ApplicationRuleViolationException {

    public DirectoryGroupLookupFailedException() {
        super("No se pudieron evaluar los grupos de Active Directory del usuario");
    }
}
