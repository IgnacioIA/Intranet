package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.User;

/**
 * {@code generatedInitialPassword} viene poblado únicamente cuando el administrador no
 * proporcionó una contraseña inicial propia — es la única oportunidad de conocerla en claro
 * (nunca se persiste ni se recupera después); comunicarla al usuario es responsabilidad del
 * administrador (fuera del alcance de esta SPEC, que no define un mecanismo de entrega).
 */
public record CreateLocalUserResult(User user, String generatedInitialPassword) {

    @Override
    public String toString() {
        return "CreateLocalUserResult[user=" + user + ", generatedInitialPassword="
                + (generatedInitialPassword == null ? "null" : "REDACTED") + "]";
    }
}
