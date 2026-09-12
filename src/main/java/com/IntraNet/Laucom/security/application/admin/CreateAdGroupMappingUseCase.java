package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** UC-AUTH-013 SPEC-AUTH-008 (alta), requiere {@code AD_MAPPING_MANAGE}. */
@Service
public class CreateAdGroupMappingUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final AdGroupRoleMappingRepositoryPort mappingRepository;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreateAdGroupMappingUseCase(AdminActionAuthorizer adminActionAuthorizer,
                                        AdGroupRoleMappingRepositoryPort mappingRepository,
                                        RoleRepositoryPort roleRepository, AuditPort auditPort, Clock clock) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.mappingRepository = mappingRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public AdGroupRoleMapping handle(UUID actorId, CreateAdGroupMappingCommand command, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.AD_MAPPING_MANAGE, correlationId);

        if (mappingRepository.findByAdGroupIdentifier(command.adGroupIdentifier()).isPresent()) {
            throw new AdGroupMappingAlreadyExistsException(); // RN-02.
        }
        Role role = roleRepository.findById(command.roleId()).orElseThrow(RoleNotFoundException::new);
        if (!role.isActive()) {
            throw new RoleInactiveException(); // RN-05.
        }

        Instant now = clock.instant();
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(
                UUID.randomUUID(), command.adGroupIdentifier(), role.id(), actorId, now);
        mappingRepository.save(mapping);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.AD_MAPPING_CHANGED, now,
                actorId, null, correlationId, AuditOutcome.SUCCESS,
                Map.of("mappingId", mapping.id().toString(), "action", "created")));
        return mapping;
    }
}
