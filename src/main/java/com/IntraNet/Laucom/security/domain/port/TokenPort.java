package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.AccessTokenClaims;
import com.IntraNet.Laucom.security.domain.model.User;

import java.util.Optional;
import java.util.Set;

/**
 * Firma/parseo del Access Token (JWT). Ver ADR-007, ADR-009. El JWT nunca es fuente de
 * verdad de autorización (ADR-006, INV-AUTH-012): solo identifica y valida la sesión.
 */
public interface TokenPort {

    AccessToken issueAccessToken(User user, Set<String> permissionNamesSnapshot);

    Optional<AccessTokenClaims> parse(String tokenValue);
}
