package com.IntraNet.Laucom.security.infrastructure.directory;

import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el único comportamiento de {@link NoOpIdentityDirectoryAdapter}: representar,
 * mediante el resultado de dominio ya existente, que Active Directory no está disponible en este
 * despliegue (ver Javadoc de la clase y ADR-002).
 */
class NoOpIdentityDirectoryAdapterTest {

    private final NoOpIdentityDirectoryAdapter adapter = new NoOpIdentityDirectoryAdapter();

    @Test
    void authenticate_alwaysReturnsDirectoryUnavailable() {
        IdentityDirectoryPort.DirectoryAuthenticationResult result =
                adapter.authenticate("jdoe", "any-password".toCharArray());

        assertThat(result).isEqualTo(new IdentityDirectoryPort.DirectoryUnavailable());
    }

    @Test
    void authenticate_isIndependentOfCredentials() {
        // No inspecciona username/password: cualquier intento de login ACTIVE_DIRECTORY con AD
        // deshabilitado debe producir el mismo resultado, sin excepción ni distinción de casos.
        IdentityDirectoryPort.DirectoryAuthenticationResult result =
                adapter.authenticate("", new char[0]);

        assertThat(result).isEqualTo(new IdentityDirectoryPort.DirectoryUnavailable());
    }
}
