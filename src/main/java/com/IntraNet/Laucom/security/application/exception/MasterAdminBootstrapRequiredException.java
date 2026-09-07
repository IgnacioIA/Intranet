package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-014 flujo 2a, RN-07 SPEC-AUTH-009, ADR-019: no existe ningún administrador local y no
 * se configuró (o no es válido) el secreto externo de bootstrap. Debe propagarse sin capturar
 * hasta abortar el arranque de la aplicación (fail-fast) — nunca debe quedar la aplicación
 * operativa sin ningún camino administrativo disponible.
 */
public final class MasterAdminBootstrapRequiredException extends ApplicationRuleViolationException {

    public MasterAdminBootstrapRequiredException(String message) {
        super(message);
    }
}
