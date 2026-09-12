package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-013 (SPEC-AUTH-008): `GET /auth/admin/ad-group-mappings`. */
@ExtendWith(MockitoExtension.class)
class ListAdGroupMappingsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private AdGroupRoleMappingRepositoryPort mappingRepository;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ListAdGroupMappingsUseCase useCase;

    @BeforeEach
    void setUp() {
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new ListAdGroupMappingsUseCase(authorizer, mappingRepository);
    }

    @Test
    void returnsEveryMapping() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW);
        when(mappingRepository.findAll()).thenReturn(List.of(mapping));

        List<AdGroupRoleMapping> result = useCase.handle(ACTOR_ID, "corr-1");

        assertThat(result).containsExactly(mapping);
    }

    @Test
    void insufficientPermission_isRejected_beforeAnythingElse() {
        doThrow(new InsufficientPermissionException()).when(authorizationService).requireAuthorized(any(), any(), any());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID, "corr-1"))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
