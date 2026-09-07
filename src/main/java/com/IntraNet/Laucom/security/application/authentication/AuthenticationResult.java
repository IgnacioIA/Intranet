package com.IntraNet.Laucom.security.application.authentication;

import com.IntraNet.Laucom.security.application.session.IssuedSession;
import com.IntraNet.Laucom.security.domain.model.User;

/**
 * Resultado de un login exitoso (UC-AUTH-001/UC-AUTH-002): el {@link User} resultante y la
 * sesión emitida (Access Token + Refresh Token, Fase 8/9). El controlador HTTP (Fase 10) es
 * quien traduce {@code session} al cuerpo de la respuesta y a la cookie {@code HttpOnly}.
 */
public record AuthenticationResult(User user, IssuedSession session) {
}
