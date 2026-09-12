package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-013 (SPEC-AUTH-008) alta de mapping. */
@ExtendWith(MockitoExtension.class)
class CreateAdGroupMappingUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private AdGroupRoleMappingRepositoryPort mappingRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private CreateAdGroupMappingUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new CreateAdGroupMappingUseCase(authorizer, mappingRepository, roleRepository, auditPort, clock);
    }

    private static Role activeRole() {
        return Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "desc");
    }

    @Test
    void createsTheMapping_whenTheGroupIsUnmapped_andTheRoleIsActive() {
        Role role = activeRole();
        when(mappingRepository.findByAdGroupIdentifier("IT-SUPPORT")).thenReturn(Optional.empty());
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        AdGroupRoleMapping mapping = useCase.handle(ACTOR_ID,
                new CreateAdGroupMappingCommand("IT-SUPPORT", role.id()), "corr-1");

        assertThat(mapping.adGroupIdentifier()).isEqualTo("IT-SUPPORT");
        assertThat(mapping.roleId()).isEqualTo(role.id());
        assertThat(mapping.createdBy()).isEqualTo(ACTOR_ID);
        verify(mappingRepository).save(mapping);
    }

    @Test
    void duplicateGroup_isRejected_withoutSaving() {
        Role role = activeRole();
        AdGroupRoleMapping existing = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", role.id(),
                UUID.randomUUID(), NOW);
        when(mappingRepository.findByAdGroupIdentifier("IT-SUPPORT")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateAdGroupMappingCommand("IT-SUPPORT", role.id()), "corr-1"))
                .isInstanceOf(AdGroupMappingAlreadyExistsException.class);

        verify(mappingRepository, never()).save(any());
        verify(roleRepository, never()).findById(any());
    }

    @Test
    void unknownRole_isRejected() {
        UUID roleId = UUID.randomUUID();
        when(mappingRepository.findByAdGroupIdentifier("IT-SUPPORT")).thenReturn(Optional.empty());
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateAdGroupMappingCommand("IT-SUPPORT", roleId), "corr-1"))
                .isInstanceOf(RoleNotFoundException.class);

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void inactiveRole_isRejected() {
        Role role = Role.reconstitute(UUID.randomUUID(), "CONTENT_EDITOR", "desc", false, false, java.util.Set.of());
        when(mappingRepository.findByAdGroupIdentifier("IT-SUPPORT")).thenReturn(Optional.empty());
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateAdGroupMappingCommand("IT-SUPPORT", role.id()), "corr-1"))
                .isInstanceOf(RoleInactiveException.class);

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateAdGroupMappingCommand("IT-SUPPORT", UUID.randomUUID()), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(mappingRepository, never()).findByAdGroupIdentifier(any());
    }

    @Test
    void audits_AD_MAPPING_CHANGED_withActorAndTimestamp() {
        Role role = activeRole();
        when(mappingRepository.findByAdGroupIdentifier("IT-SUPPORT")).thenReturn(Optional.empty());
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        useCase.handle(ACTOR_ID, new CreateAdGroupMappingCommand("IT-SUPPORT", role.id()), "corr-1");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(auditPort, org.mockito.Mockito.times(2)).record(captor.capture()); // + AUTHORIZATION_GRANTED (RN-16).
        SecurityAuditEvent event = captor.getAllValues().stream()
                .filter(e -> e.eventType() == SecurityEventType.AD_MAPPING_CHANGED)
                .findFirst().orElseThrow();
        assertThat(event.actorUserId()).contains(ACTOR_ID);
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }
}
