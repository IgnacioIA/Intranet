package com.IntraNet.Laucom.security.domain.port;

import java.util.Set;

/**
 * Abstrae el directorio de identidad externo (Active Directory u otro). Ver ADR-002,
 * SPEC-AUTH-001. Nombrado de forma abstracta a propósito: no acopla el dominio a AD/LDAP.
 */
public interface IdentityDirectoryPort {

    DirectoryAuthenticationResult authenticate(String username, char[] password);

    /** Resultado sellado: modela explícitamente los flujos alternativos de UC-AUTH-002. */
    sealed interface DirectoryAuthenticationResult
            permits Authenticated, CredentialsRejected, DirectoryUnavailable, GroupLookupFailed {
    }

    record Authenticated(String externalId, String displayName, String email, Set<String> groupIdentifiers)
            implements DirectoryAuthenticationResult {
    }

    /** UC-AUTH-002, flujo 2a: credenciales rechazadas por el directorio. */
    record CredentialsRejected() implements DirectoryAuthenticationResult {
    }

    /** UC-AUTH-002, flujo 2b: directorio inalcanzable (AD_CONNECTION_FAILURE). */
    record DirectoryUnavailable() implements DirectoryAuthenticationResult {
    }

    /**
     * UC-AUTH-002, flujo 3a — ADR-017 (fail closed): credenciales correctas pero falla la
     * obtención/evaluación de grupos. No debe tratarse como {@link CredentialsRejected}.
     */
    record GroupLookupFailed(String externalId) implements DirectoryAuthenticationResult {
    }
}
