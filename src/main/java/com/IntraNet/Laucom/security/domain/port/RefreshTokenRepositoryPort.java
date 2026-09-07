package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de {@link RefreshToken}. Ver SPEC-AUTH-002, ADR-008, INV-AUTH-006, INV-AUTH-007.
 *
 * <p>Las dos operaciones de revocación masiva ({@code revokeAllActiveInFamily},
 * {@code revokeAllActiveForUser}) se expresan como comandos propios del Port, en vez de
 * "cargar todos + mutar + guardar uno por uno", porque pueden afectar muchas filas a la vez
 * (una familia entera de rotaciones, o todas las sesiones de un usuario) y deben ejecutarse
 * como una única operación atómica en el adapter (Fase 9), no como un bucle en la capa de
 * aplicación.</p>
 */
public interface RefreshTokenRepositoryPort {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Primera emisión de una familia nueva (login, SessionIssuer): una única fila. */
    RefreshToken save(RefreshToken token);

    /**
     * INV-AUTH-006: persiste el token padre ya revocado (con {@code replacedByTokenId} fijado)
     * y su reemplazo nuevo como una única operación atómica — nunca dos llamadas independientes
     * a {@link #save}, que dejarían a criterio de un componente externo (no de este Port)
     * garantizar que ambas ocurran juntas o ninguna ocurra.
     */
    void saveRotation(RefreshToken revokedParent, RefreshToken newChild);

    /** UC-AUTH-005 3b, INV-AUTH-007: reutilización detectada. */
    void revokeAllActiveInFamily(UUID familyId, Instant revokedAt);

    /** SessionRevocationPort (RN-05 SPEC-AUTH-007, REQ-AUTH-011 aplicado automáticamente). */
    void revokeAllActiveForUser(UUID userId, Instant revokedAt);
}
