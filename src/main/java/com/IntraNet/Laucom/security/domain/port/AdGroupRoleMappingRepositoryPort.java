package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistencia de {@link AdGroupRoleMapping}. Ver SPEC-AUTH-001 (UC-AUTH-003/004), SPEC-AUTH-008.
 *
 * <p>{@code findByAdGroupIdentifierIn} sirve la sincronización de login AD (Fase 5). Los métodos
 * de administración (UC-AUTH-013, SPEC-AUTH-008) se agregaron cuando esa administración se
 * implementó — ya no son especulativos.</p>
 */
public interface AdGroupRoleMappingRepositoryPort {

    /** INV-AUTH-010: como máximo un mapping por identificador de grupo. */
    Set<AdGroupRoleMapping> findByAdGroupIdentifierIn(Set<String> adGroupIdentifiers);

    Optional<AdGroupRoleMapping> findById(UUID id);

    /** UC-AUTH-013 SPEC-AUTH-008 RN-02: verificación de unicidad en alta/modificación. */
    Optional<AdGroupRoleMapping> findByAdGroupIdentifier(String adGroupIdentifier);

    /** UC-AUTH-013 SPEC-AUTH-008: `GET /auth/admin/ad-group-mappings`. Catálogo acotado, sin paginación. */
    List<AdGroupRoleMapping> findAll();

    /**
     * @throws com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException
     * si la violación de la constraint {@code UNIQUE(ad_group_identifier)} (INV-AUTH-010) llega
     * a nivel de base de datos pese a la verificación previa de la capa de aplicación (condición
     * de carrera entre dos altas concurrentes para el mismo grupo).
     */
    AdGroupRoleMapping save(AdGroupRoleMapping mapping);

    /** UC-AUTH-013 SPEC-AUTH-008: baja física — INV-AUTH-014 (soft deactivation) no aplica a este tipo. */
    void deleteById(UUID id);
}
