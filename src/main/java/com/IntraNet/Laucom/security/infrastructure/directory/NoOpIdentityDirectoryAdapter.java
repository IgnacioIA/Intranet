package com.IntraNet.Laucom.security.infrastructure.directory;

import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de {@link IdentityDirectoryPort} activo cuando Active Directory está deshabilitado o
 * no configurado en este despliegue (ADR-002: "un proyecto sin AD simplemente no configura este
 * adapter"). Existe porque {@code AuthenticateActiveDirectoryUserUseCase} y
 * {@code AuthenticationController} declaran {@link IdentityDirectoryPort} como dependencia
 * obligatoria de constructor: sin un bean que lo satisfaga, Spring no puede levantar el contexto
 * cuando {@code ad.enabled=false} (o está ausente), aunque AD nunca se haya pedido para este
 * despliegue.
 *
 * <p>Devuelve siempre {@link IdentityDirectoryPort.DirectoryUnavailable}, el mismo resultado de
 * dominio que {@link ActiveDirectoryAdapter} produce cuando el directorio real es inalcanzable en
 * runtime (UC-AUTH-002 flujo 2b, {@code AD_CONNECTION_FAILURE}). Es la representación correcta
 * también para este caso: para {@code AuthenticateActiveDirectoryUserUseCase} y para el cliente
 * HTTP, "AD no está configurado en este despliegue" y "AD está temporalmente inalcanzable" son
 * indistinguibles — ambas significan "el servicio de autenticación de directorio no está
 * disponible ahora", que es exactamente la única información que SPEC-AUTH-001 (RN-06, excepción
 * intencional) autoriza a exponer aquí: la disponibilidad del servicio no es información de la
 * cuenta. Por eso este adapter reutiliza ese resultado de dominio en vez de introducir un
 * concepto nuevo.</p>
 *
 * <p>Mutuamente excluyente con {@link ActiveDirectoryAdapter} vía {@code @ConditionalOnProperty}:
 * {@code havingValue="false", matchIfMissing=true} cubre exactamente los dos casos en los que
 * {@link ActiveDirectoryAdapter} (condicionado a {@code ad.enabled=true}) no se registra —
 * property explícita en {@code false} o ausente — de modo que exista siempre exactamente un bean
 * de {@link IdentityDirectoryPort} en el contexto.</p>
 */
@Component
@ConditionalOnProperty(name = "ad.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpIdentityDirectoryAdapter implements IdentityDirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpIdentityDirectoryAdapter.class);

    @Override
    public DirectoryAuthenticationResult authenticate(String username, char[] password) {
        log.debug("Intento de login ACTIVE_DIRECTORY con Active Directory deshabilitado en este "
                + "despliegue (ad.enabled=false o ausente)");
        return new DirectoryUnavailable();
    }
}
