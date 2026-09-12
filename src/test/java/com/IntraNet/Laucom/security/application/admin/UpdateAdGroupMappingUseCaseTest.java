package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingNotFoundException;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/** Cubre UC-AUTH-013 (SPEC-AUTH-008) modificación de mapping. */
@ExtendWith(MockitoExtension.class)
class UpdateAdGroupMappingUseCaseTest {

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
    private UpdateAdGroupMappingUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new UpdateAdGroupMappingUseCase(authorizer, mappingRepository, roleRepository, auditPort, clock);
    }

    private static Role activeRole(String name) {
        return Role.create(UUID.randomUUID(), name, "desc");
    }

    @Test
    void changesTheRole_keepingTheSameGroupIdentifier() {
        Role originalRole = activeRole("CONTENT_EDITOR");
        Role newRole = activeRole("MANAGER");
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", originalRole.id(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));
        when(roleRepository.findById(newRole.id())).thenReturn(Optional.of(newRole));

        AdGroupRoleMapping result = useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mapping.id(), "IT-SUPPORT", newRole.id()), "corr-1");

        assertThat(result.adGroupIdentifier()).isEqualTo("IT-SUPPORT");
        assertThat(result.roleId()).isEqualTo(newRole.id());
        verify(mappingRepository, never()).findByAdGroupIdentifier(any()); // identificador sin cambios: sin chequeo de unicidad.
    }

    @Test
    void changingTheGroupIdentifier_toOneAlreadyUsedByAnotherMapping_isRejected() {
        Role role = activeRole("CONTENT_EDITOR");
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", role.id(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        AdGroupRoleMapping other = AdGroupRoleMapping.create(UUID.randomUUID(), "SALES", role.id(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));
        when(mappingRepository.findByAdGroupIdentifier("SALES")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mapping.id(), "SALES", role.id()), "corr-1"))
                .isInstanceOf(AdGroupMappingAlreadyExistsException.class);

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void changingTheGroupIdentifier_toOneOnlyUsedByItself_succeeds() {
        // Caso límite: el propio mapping ya tiene ese identificador (no-op real) — no debe
        // rechazarse como si fuera un conflicto contra sí mismo.
        Role role = activeRole("CONTENT_EDITOR");
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", role.id(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));
        when(mappingRepository.findByAdGroupIdentifier("NEW-NAME")).thenReturn(Optional.empty());
        when(roleRepository.findById(role.id())).thenReturn(Optional.of(role));

        AdGroupRoleMapping result = useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mapping.id(), "NEW-NAME", role.id()), "corr-1");

        assertThat(result.adGroupIdentifier()).isEqualTo("NEW-NAME");
    }

    @Test
    void unknownMapping_throwsMappingNotFound() {
        UUID mappingId = UUID.randomUUID();
        when(mappingRepository.findById(mappingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mappingId, "IT-SUPPORT", UUID.randomUUID()), "corr-1"))
                .isInstanceOf(AdGroupMappingNotFoundException.class);
    }

    @Test
    void unknownRole_isRejected() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        UUID unknownRoleId = UUID.randomUUID();
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));
        when(roleRepository.findById(unknownRoleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mapping.id(), "IT-SUPPORT", unknownRoleId), "corr-1"))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void inactiveRole_isRejected() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW.minusSeconds(1000));
        Role inactiveRole = Role.reconstitute(UUID.randomUUID(), "MANAGER", "desc", false, false, java.util.Set.of());
        when(mappingRepository.findById(mapping.id())).thenReturn(Optional.of(mapping));
        when(roleRepository.findById(inactiveRole.id())).thenReturn(Optional.of(inactiveRole));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(mapping.id(), "IT-SUPPORT", inactiveRole.id()), "corr-1"))
                .isInstanceOf(RoleInactiveException.class);
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new UpdateAdGroupMappingCommand(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID()), "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(mappingRepository, never()).findById(any());
    }
}
