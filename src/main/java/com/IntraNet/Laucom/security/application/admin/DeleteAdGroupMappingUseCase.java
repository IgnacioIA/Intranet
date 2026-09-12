package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.AdGroupMappingNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-013 SPEC-AUTH-008 (baja), requiere {@code AD_MAPPING_MANAGE}. RN-04: no revoca
 * retroactivamente ningún rol ya derivado — el efecto se aplica recién en la siguiente
 * sincronización de cada usuario afectado (`SPEC-AUTH-001`), por lo que este Use Case no
 * recorre usuarios existentes.
 */
@Service
public class DeleteAdGroupMappingUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final AdGroupRoleMappingRepositoryPort mappingRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public DeleteAdGroupMappingUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                        AdGroupRoleMappingRepositoryPort mappingRepository,
                                        AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.mappingRepository = mappingRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public void handle(UUID actorId, UUID mappingId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.AD_MAPPING_MANAGE, correlationId);

        AdGroupRoleMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(AdGroupMappingNotFoundException::new);
        mappingRepository.deleteById(mapping.id());

        Instant now = clock.instant();
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.AD_MAPPING_CHANGED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS,
                Map.of("mappingId", mapping.id().toString(), "action", "deleted")));
    }
}
