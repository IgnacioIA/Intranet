package com.IntraNet.Laucom.security.application.exception;

import com.IntraNet.Laucom.security.domain.service.PasswordPolicy;

public final class PasswordPolicyViolationException extends ApplicationRuleViolationException {

    public PasswordPolicyViolationException() {
        super("La nueva contraseña no cumple la política vigente (mínimo " + PasswordPolicy.MIN_LENGTH + " caracteres)");
    }
}
