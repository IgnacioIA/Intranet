package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.application.admin.CreateLocalUserResult;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;

/**
 * SPEC-AUTH-010 §12. {@code generatedInitialPassword} solo viene poblado cuando el
 * administrador no proporcionó una contraseña propia — única oportunidad de conocerla en claro.
 */
public record CreateUserResponse(String id, String provider, String username, String status,
                                  boolean mustChangeOnNextLogin, String generatedInitialPassword) {

    public static CreateUserResponse from(CreateLocalUserResult result) {
        boolean mustChange = result.user().credential().map(PasswordCredential::mustChangeOnNextLogin).orElse(false);
        return new CreateUserResponse(result.user().id().toString(), result.user().provider().name(),
                result.user().username(), result.user().status().name(), mustChange, result.generatedInitialPassword());
    }
}
