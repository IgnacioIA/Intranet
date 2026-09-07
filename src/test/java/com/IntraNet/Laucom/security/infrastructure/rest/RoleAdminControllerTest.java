package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreateRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.GetRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.ListRolesUseCase;
import com.IntraNet.Laucom.security.application.admin.SetRoleActiveUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateRoleUseCase;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.RoleAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.domain.exception.SystemRoleProtectedException;
import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC-AUTH-010 §12 ("Roles"). `standaloneSetup`, mismo criterio que el resto de los
 * controladores REST del módulo: verifica el mapeo HTTP y de errores, no la autorización real ni
 * las reglas de negocio (ya cubiertas exhaustivamente a nivel de Use Case en
 * `application.admin.*Test`).
 */
@ExtendWith(MockitoExtension.class)
class RoleAdminControllerTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private ListRolesUseCase listRoles;
    @Mock
    private GetRoleUseCase getRole;
    @Mock
    private CreateRoleUseCase createRole;
    @Mock
    private UpdateRoleUseCase updateRole;
    @Mock
    private SetRoleActiveUseCase setRoleActive;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RoleAdminController controller = new RoleAdminController(listRoles, getRole, createRole, updateRole, setRoleActive);
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

    private static Role aRole() {
        Role role = Role.create(UUID.randomUUID(), "CONTENT_EDITOR", "Edita contenido");
        role.grant(Permission.create("CONTENT_READ", "Leer"));
        return role;
    }

    @Test
    void list_returnsEveryRole() throws Exception {
        when(listRoles.handle(any(), any())).thenReturn(List.of(aRole()));

        mockMvc.perform(get("/auth/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CONTENT_EDITOR"))
                .andExpect(jsonPath("$[0].permissions[0]").value("CONTENT_READ"));
    }

    @Test
    void detail_returnsTheRole() throws Exception {
        Role role = aRole();
        when(getRole.handle(any(), any(), any())).thenReturn(role);

        mockMvc.perform(get("/auth/admin/roles/" + role.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(role.id().toString()));
    }

    @Test
    void detail_unknownRole_returns404() throws Exception {
        when(getRole.handle(any(), any(), any())).thenThrow(new RoleNotFoundException());

        mockMvc.perform(get("/auth/admin/roles/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("role-not-found")));
    }

    @Test
    void create_returns201_withTheCreatedRole() throws Exception {
        when(createRole.handle(any(), any(), any())).thenReturn(aRole());

        mockMvc.perform(post("/auth/admin/roles")
                        .contentType("application/json")
                        .content("{\"name\":\"CONTENT_EDITOR\",\"description\":\"Edita contenido\",\"permissions\":[\"CONTENT_READ\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("CONTENT_EDITOR"));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        when(createRole.handle(any(), any(), any())).thenThrow(new RoleAlreadyExistsException());

        mockMvc.perform(post("/auth/admin/roles")
                        .contentType("application/json")
                        .content("{\"name\":\"CONTENT_EDITOR\",\"description\":\"desc\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("role-already-exists")));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/auth/admin/roles")
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsTheUpdatedRole() throws Exception {
        Role role = aRole();
        when(updateRole.handle(any(), any(), any())).thenReturn(role);

        mockMvc.perform(put("/auth/admin/roles/" + role.id())
                        .contentType("application/json")
                        .content("{\"name\":\"ignored\",\"description\":\"nueva\",\"permissions\":[\"CONTENT_READ\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CONTENT_EDITOR"));
    }

    @Test
    void deactivate_systemRole_returns409() throws Exception {
        when(setRoleActive.handle(any(), any(), org.mockito.ArgumentMatchers.eq(false), any()))
                .thenThrow(new SystemRoleProtectedException("MASTER_ADMIN"));

        mockMvc.perform(post("/auth/admin/roles/" + UUID.randomUUID() + "/deactivate"))
                .andExpect(status().isConflict());
    }

    @Test
    void activate_returnsTheReactivatedRole() throws Exception {
        Role role = aRole();
        when(setRoleActive.handle(any(), any(), org.mockito.ArgumentMatchers.eq(true), any())).thenReturn(role);

        mockMvc.perform(post("/auth/admin/roles/" + role.id() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void insufficientPermission_returns403() throws Exception {
        when(listRoles.handle(any(), any())).thenThrow(new InsufficientPermissionException());

        mockMvc.perform(get("/auth/admin/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(containsString("insufficient-permissions")));
    }
}
