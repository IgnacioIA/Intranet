package com.IntraNet.Laucom.security.application.exception;

/**
 * RN-01 SPEC-AUTH-007 / REQ-AUTH-012 (cambio de contraseña) y RN-02 SPEC-AUTH-010 (actualización
 * de identidad, UC-AUTH-017): un usuario {@code ACTIVE_DIRECTORY} fue objeto de una operación
 * (contraseña o identidad) que pertenece exclusivamente a Active Directory, no a la aplicación.
 * Mismo tipo de error RFC 7807 en ambas SPECs ({@code identity-managed-externally}).
 */
public final class IdentityManagedExternallyException extends ApplicationRuleViolationException {

    public IdentityManagedExternallyException() {
        super("Este atributo de la identidad es gestionado por Active Directory, no por la aplicación");
    }
}
