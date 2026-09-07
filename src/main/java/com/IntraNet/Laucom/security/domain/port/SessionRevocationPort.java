package com.IntraNet.Laucom.security.domain.port;

import java.util.UUID;

/**
 * Revoca todas las sesiones (familias de Refresh Token) activas de un usuario. Ver RN-05
 * SPEC-AUTH-007, y el efecto colateral obligatorio documentado en
 * docs/02-domain/transitions.md para los cambios de estado de {@code User}.
 *
 * <p>La implementación concreta se agrega en Fase 9 (Refresh Tokens), una vez exista la
 * persistencia de {@code RefreshToken} (Fase 2 no la incluyó — ver su checkpoint). Hasta
 * entonces, ningún adapter Spring satisface este Port; los Use Cases que lo usan (Fase 3) se
 * verifican con Ports mockeados, no con el contexto de Spring completo.</p>
 */
public interface SessionRevocationPort {

    void revokeAllSessions(UUID userId);
}
