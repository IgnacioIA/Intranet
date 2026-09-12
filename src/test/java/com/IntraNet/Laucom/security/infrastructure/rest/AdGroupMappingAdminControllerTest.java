package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreateAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.application.admin.DeleteAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.application.admin.ListAdGroupMappingsUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingNotFoundException;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC-AUTH-008 §12. `standaloneSetup`, mismo criterio que el resto de los controladores REST
 * del módulo: verifica el mapeo HTTP y de errores, no las reglas de negocio (ya cubiertas a
 * nivel de Use Case en `application.admin.*AdGroupMapping*Test`).
 */
@ExtendWith(MockitoExtension.class)
class AdGroupMappingAdminControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private ListAdGroupMappingsUseCase listMappings;
    @Mock
    private CreateAdGroupMappingUseCase createMapping;
    @Mock
    private UpdateAdGroupMappingUseCase updateMapping;
    @Mock
    private DeleteAdGroupMappingUseCase deleteMapping;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdGroupMappingAdminController controller =
                new AdGroupMappingAdminController(listMappings, createMapping, updateMapping, deleteMapping);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AuthenticationExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(ACTOR_ID, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static AdGroupRoleMapping aMapping() {
        return AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(), ACTOR_ID, NOW);
    }

    @Test
    void list_returnsEveryMapping() throws Exception {
        when(listMappings.handle(any(), any())).thenReturn(List.of(aMapping()));

        mockMvc.perform(get("/auth/admin/ad-group-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adGroupIdentifier").value("IT-SUPPORT"));
    }

    @Test
    void create_returns201_withTheCreatedMapping() throws Exception {
        when(createMapping.handle(any(), any(), any())).thenReturn(aMapping());

        mockMvc.perform(post("/auth/admin/ad-group-mappings")
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adGroupIdentifier").value("IT-SUPPORT"));
    }

    @Test
    void create_duplicateGroup_returns409() throws Exception {
        when(createMapping.handle(any(), any(), any())).thenThrow(new AdGroupMappingAlreadyExistsException());

        mockMvc.perform(post("/auth/admin/ad-group-mappings")
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("group-already-mapped")));
    }

    @Test
    void create_unknownRole_returns404() throws Exception {
        when(createMapping.handle(any(), any(), any())).thenThrow(new RoleNotFoundException());

        mockMvc.perform(post("/auth/admin/ad-group-mappings")
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("role-not-found")));
    }

    @Test
    void create_inactiveRole_returns400() throws Exception {
        when(createMapping.handle(any(), any(), any())).thenThrow(new RoleInactiveException());

        mockMvc.perform(post("/auth/admin/ad-group-mappings")
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("role-inactive")));
    }

    @Test
    void create_blankGroupIdentifier_returns400() throws Exception {
        mockMvc.perform(post("/auth/admin/ad-group-mappings")
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsTheUpdatedMapping() throws Exception {
        AdGroupRoleMapping mapping = aMapping();
        when(updateMapping.handle(any(), any(), any())).thenReturn(mapping);

        mockMvc.perform(put("/auth/admin/ad-group-mappings/" + mapping.id())
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adGroupIdentifier").value("IT-SUPPORT"));
    }

    @Test
    void update_unknownMapping_returns404() throws Exception {
        when(updateMapping.handle(any(), any(), any())).thenThrow(new AdGroupMappingNotFoundException());

        mockMvc.perform(put("/auth/admin/ad-group-mappings/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"IT-SUPPORT\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("mapping-not-found")));
    }

    @Test
    void update_duplicateGroup_returns409() throws Exception {
        when(updateMapping.handle(any(), any(), any())).thenThrow(new AdGroupMappingAlreadyExistsException());

        mockMvc.perform(put("/auth/admin/ad-group-mappings/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"adGroupIdentifier\":\"SALES\",\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("group-already-mapped")));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/auth/admin/ad-group-mappings/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_unknownMapping_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new AdGroupMappingNotFoundException())
                .when(deleteMapping).handle(any(), any(), any());

        mockMvc.perform(delete("/auth/admin/ad-group-mappings/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("mapping-not-found")));
    }

    @Test
    void insufficientPermission_returns403() throws Exception {
        when(listMappings.handle(any(), any())).thenThrow(new InsufficientPermissionException());

        mockMvc.perform(get("/auth/admin/ad-group-mappings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(containsString("insufficient-permissions")));
    }
}
