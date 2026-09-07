package com.IntraNet.Laucom.security.application.exception;

/**
 * UC-AUTH-002 flujo 2b: Active Directory inalcanzable. A diferencia de
 * {@link InvalidCredentialException}, esta condición es <b>intencionalmente distinguible</b>
 * (RN-06 SPEC-AUTH-001 no aplica aquí: la disponibilidad del servicio no es información de la
 * cuenta). La capa API (Fase 19) la mapea a 503 + {@code ad-unavailable}.
 */
public final class DirectoryUnavailableException extends ApplicationRuleViolationException {

    public DirectoryUnavailableException() {
        super("Servicio de autenticación de directorio no disponible");
    }
}
