package com.IntraNet.Laucom.security.application.session;

/** UC-AUTH-005 SPEC-AUTH-002. El secreto en claro viaja por cookie (Fase 10), nunca por cuerpo. */
public record RenewSessionCommand(String refreshTokenSecret) {
}
