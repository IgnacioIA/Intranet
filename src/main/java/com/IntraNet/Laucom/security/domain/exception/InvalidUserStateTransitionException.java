package com.IntraNet.Laucom.security.domain.exception;

import com.IntraNet.Laucom.security.domain.model.UserStatus;

/**
 * RN-03 SPEC-AUTH-010: transición no contemplada en docs/02-domain/transitions.md.
 */
public final class InvalidUserStateTransitionException extends DomainRuleViolationException {

    private final UserStatus currentStatus;
    private final String attemptedOperation;

    public InvalidUserStateTransitionException(UserStatus currentStatus, String attemptedOperation) {
        super("Transición inválida: no se puede ejecutar '" + attemptedOperation + "' desde el estado " + currentStatus);
        this.currentStatus = currentStatus;
        this.attemptedOperation = attemptedOperation;
    }

    public UserStatus currentStatus() {
        return currentStatus;
    }

    public String attemptedOperation() {
        return attemptedOperation;
    }
}
