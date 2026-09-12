package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingNotFoundException;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
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

/**
 * Cubre UC-AUTH-013 (SPEC-AUTH-008) baja de mapping. RN-04: no debe recorrer usuarios
 * existentes — solo verifica que este Use Case no depende de {@code UserRepositoryPort} en
 * absoluto (no lo declara como colaborador).
 */
@ExtendWith(MockitoExtension.class)
class DeleteAdGroupMappingUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private AdGroupRoleMappingRepositoryPort mappingRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private DeleteAdGroupMappingUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new DeleteAdGroupMappingUseCase(authorizer, mappingRepository, auditPort, clock);
    }

    @Test
    void deletesTheMapping() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));

        useCase.handle(ACTOR_ID, mapping.id(), "corr-1");

        verify(mappingRepository).deleteById(mapping.id());
    }

    @Test
    void unknownMapping_throwsMappingNotFound_withoutDeleting() {
        UUID mappingId = UUID.randomUUID();
        when(mappingRepository.findById(mappingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, mappingId, "corr-1"))
                .isInstanceOf(AdGroupMappingNotFoundException.class);

        verify(mappingRepository, never()).deleteById(any());
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, UUID.randomUUID(), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(mappingRepository, never()).findById(any());
    }

    @Test
    void audits_AD_MAPPING_CHANGED_withActorAndTimestamp() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));

        useCase.handle(ACTOR_ID, mapping.id(), "corr-1");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(auditPort, org.mockito.Mockito.times(2)).record(captor.capture());
        SecurityAuditEvent event = captor.getAllValues().stream()
                .filter(e -> e.eventType() == SecurityEventType.AD_MAPPING_CHANGED)
                .findFirst().orElseThrow();
        assertThat(event.actorUserId()).contains(ACTOR_ID);
        assertThat(event.metadata()).containsEntry("mappingId", mapping.id().toString());
    }
}
