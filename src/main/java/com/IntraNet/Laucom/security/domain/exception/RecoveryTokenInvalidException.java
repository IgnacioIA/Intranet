package com.IntraNet.Laucom.security.domain.exception;

/**
 * INV-AUTH-008: un {@link com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken}
 * ya usado o expirado no puede autorizar el establecimiento de una nueva contraseña.
 */
public final class RecoveryTokenInvalidException extends DomainRuleViolationException {

    public RecoveryTokenInvalidException(String reason) {
        super("Token de recuperación inválido: " + reason);
    }
}
