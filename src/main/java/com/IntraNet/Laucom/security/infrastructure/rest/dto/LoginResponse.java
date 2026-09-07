package com.IntraNet.Laucom.security.infrastructure.rest.dto;

import com.IntraNet.Laucom.security.domain.model.AccessToken;

/** SPEC-AUTH-001 §12 / SPEC-AUTH-002 §12: cuerpo de respuesta común a login y refresh. */
public record LoginResponse(String accessToken, long expiresInSeconds, String tokenType) {

    public static LoginResponse of(AccessToken accessToken, long expiresInSeconds) {
        return new LoginResponse(accessToken.value(), expiresInSeconds, "Bearer");
    }
}
