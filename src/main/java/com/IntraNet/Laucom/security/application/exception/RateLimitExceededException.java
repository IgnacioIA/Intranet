package com.IntraNet.Laucom.security.application.exception;

/** REQ-AUTH-018. La capa API (Fase 19) mapea esta excepción a 429 + RATE_LIMIT_EXCEEDED. */
public final class RateLimitExceededException extends ApplicationRuleViolationException {

    public RateLimitExceededException() {
        super("Límite de intentos excedido");
    }
}
