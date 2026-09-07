package com.IntraNet.Laucom.security.domain.exception;

/**
 * INV-AUTH-007: se intentó canjear un {@link com.IntraNet.Laucom.security.domain.model.RefreshToken}
 * que ya no está vigente (expirado o revocado). La capa de aplicación decide, a partir de esta
 * excepción, si corresponde revocar el resto de la familia (reuse detection).
 */
public final class RefreshTokenNotActiveException extends DomainRuleViolationException {

    public RefreshTokenNotActiveException() {
        super("El Refresh Token ya no está vigente (expirado o revocado)");
    }
}
