package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreatePermissionUseCase;
import com.IntraNet.Laucom.security.application.admin.GetPermissionUseCase;
import com.IntraNet.Laucom.security.application.admin.ListPermissionsUseCase;
import com.IntraNet.Laucom.security.application.admin.SetPermissionActiveUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdatePermissionDescriptionUseCase;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.PermissionAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.PermissionNameImmutableException;
import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.domain.exception.SystemPermissionProtectedException;
import com.IntraNet.Laucom.security.domain.model.Permission;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC-AUTH-010 §12 ("Permissions"). `standaloneSetup`, mismo criterio que el resto de los
 * controladores REST del módulo: verifica el mapeo HTTP y de errores, no la autorización real ni
 * las reglas de negocio (ya cubiertas exhaustivamente a nivel de Use Case en
 * `application.admin.*Test`).
 */
@ExtendWith(MockitoExtension.class)
class PermissionAdminControllerTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private ListPermissionsUseCase listPermissions;
    @Mock
    private GetPermissionUseCase getPermission;
    @Mock
    private CreatePermissionUseCase createPermission;
    @Mock
    private UpdatePermissionDescriptionUseCase updatePermissionDescription;
    @Mock
    private SetPermissionActiveUseCase setPermissionActive;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PermissionAdminController controller = new PermissionAdminController(listPermissions, getPermission,
                createPermission, updatePermissionDescription, setPermissionActive);
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

    private static Permission aPermission() {
        return Permission.create("CONTENT_READ", "Leer contenido");
    }

    @Test
    void list_returnsEveryPermission() throws Exception {
        when(listPermissions.handle(any(), any())).thenReturn(List.of(aPermission()));

        mockMvc.perform(get("/auth/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CONTENT_READ"));
    }

    @Test
    void detail_returnsThePermission() throws Exception {
        when(getPermission.handle(any(), any(), any())).thenReturn(aPermission());

        mockMvc.perform(get("/auth/admin/permissions/CONTENT_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CONTENT_READ"))
                .andExpect(jsonPath("$.name").value("CONTENT_READ"));
    }

    @Test
    void detail_unknownPermission_returns404() throws Exception {
        when(getPermission.handle(any(), any(), any())).thenThrow(new PermissionNotFoundException());

        mockMvc.perform(get("/auth/admin/permissions/DOES_NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("permission-not-found")));
    }

    @Test
    void create_returns201_withTheCreatedPermission() throws Exception {
        when(createPermission.handle(any(), any(), any())).thenReturn(aPermission());

        mockMvc.perform(post("/auth/admin/permissions")
                        .contentType("application/json")
                        .content("{\"name\":\"CONTENT_READ\",\"description\":\"Leer contenido\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("CONTENT_READ"));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        when(createPermission.handle(any(), any(), any())).thenThrow(new PermissionAlreadyExistsException());

        mockMvc.perform(post("/auth/admin/permissions")
                        .contentType("application/json")
                        .content("{\"name\":\"CONTENT_READ\",\"description\":\"desc\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("permission-already-exists")));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/auth/admin/permissions")
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsTheUpdatedPermission() throws Exception {
        when(updatePermissionDescription.handle(any(), any(), any())).thenReturn(aPermission());

        mockMvc.perform(patch("/auth/admin/permissions/CONTENT_READ")
                        .contentType("application/json")
                        .content("{\"description\":\"nueva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CONTENT_READ"));
    }

    @Test
    void update_attemptingToRenameIt_returns400() throws Exception {
        when(updatePermissionDescription.handle(any(), any(), any())).thenThrow(new PermissionNameImmutableException());

        mockMvc.perform(patch("/auth/admin/permissions/CONTENT_READ")
                        .contentType("application/json")
                        .content("{\"name\":\"OTHER_NAME\",\"description\":\"nueva\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("permission-name-immutable")));
    }

    @Test
    void deactivate_systemPermission_returns409() throws Exception {
        when(setPermissionActive.handle(any(), any(), eq(false), any()))
                .thenThrow(new SystemPermissionProtectedException("VIEW_ONBOARDING_INFO"));

        mockMvc.perform(post("/auth/admin/permissions/VIEW_ONBOARDING_INFO/deactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("system-permission-protected")));
    }

    @Test
    void activate_returnsTheReactivatedPermission() throws Exception {
        when(setPermissionActive.handle(any(), any(), eq(true), any())).thenReturn(aPermission());

        mockMvc.perform(post("/auth/admin/permissions/CONTENT_READ/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void insufficientPermission_returns403() throws Exception {
        when(listPermissions.handle(any(), any())).thenThrow(new InsufficientPermissionException());

        mockMvc.perform(get("/auth/admin/permissions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(containsString("insufficient-permissions")));
    }
}
