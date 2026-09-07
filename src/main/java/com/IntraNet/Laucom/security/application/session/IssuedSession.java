package com.IntraNet.Laucom.security.application.session;

import com.IntraNet.Laucom.security.domain.model.AccessToken;

import java.time.Instant;

/**
 * Resultado de emitir o renovar una sesión: el Access Token (JWT, Fase 8) y el valor en claro
 * del nuevo Refresh Token (Fase 9) — este último nunca se persiste tal cual (solo su hash) y
 * está pensado para que la capa de transporte (Fase 10) lo coloque en la cookie {@code HttpOnly}.
 */
public record IssuedSession(AccessToken accessToken, String refreshTokenSecret, Instant refreshTokenExpiresAt) {

    @Override
    public String toString() {
        return "IssuedSession[accessToken=" + accessToken + ", refreshTokenSecret=REDACTED, "
                + "refreshTokenExpiresAt=" + refreshTokenExpiresAt + "]";
    }
}
