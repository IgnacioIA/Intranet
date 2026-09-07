package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;

import java.util.Set;

/**
 * Persistencia de {@link AdGroupRoleMapping}. Ver SPEC-AUTH-001 (UC-AUTH-003/004), SPEC-AUTH-008.
 *
 * <p>Solo expone, por ahora, el método que UC-AUTH-003/UC-AUTH-004 (Fase 5) requieren
 * genuinamente: resolver qué grupos AD de un usuario tienen mapping vigente. Los métodos de
 * alta/baja/modificación (UC-AUTH-013, SPEC-AUTH-008) se agregan cuando esa administración se
 * implemente — requiere autorización (`AD_MAPPING_MANAGE`), que todavía no existe (Fase 6) — no
 * se anticipan aquí de forma especulativa (mismo criterio que {@code UserRepositoryPort}).</p>
 */
public interface AdGroupRoleMappingRepositoryPort {

    /** INV-AUTH-010: como máximo un mapping por identificador de grupo. */
    Set<AdGroupRoleMapping> findByAdGroupIdentifierIn(Set<String> adGroupIdentifiers);
}
