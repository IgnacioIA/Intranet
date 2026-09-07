package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.AssignRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.ChangeUserStatusUseCase;
import com.IntraNet.Laucom.security.application.admin.CreateLocalUserResult;
import com.IntraNet.Laucom.security.application.admin.CreateLocalUserUseCase;
import com.IntraNet.Laucom.security.application.admin.GetUserDetailUseCase;
import com.IntraNet.Laucom.security.application.admin.ListUserRoleAssignmentsUseCase;
import com.IntraNet.Laucom.security.application.admin.ListUsersUseCase;
import com.IntraNet.Laucom.security.application.admin.RevokeRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateLocalUserIdentityUseCase;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.application.exception.UsernameAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.PageResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC-AUTH-010 §12 ("Usuarios"). `standaloneSetup`, mismo criterio que el resto de los
 * controladores REST del módulo: verifica el mapeo HTTP y de errores, no la autorización real
 * (ya cubierta exhaustivamente a nivel de Use Case en `application.admin.*Test`).
 */
@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private ListUsersUseCase listUsers;
    @Mock
    private GetUserDetailUseCase getUserDetail;
    @Mock
    private CreateLocalUserUseCase createLocalUser;
    @Mock
    private UpdateLocalUserIdentityUseCase updateLocalUserIdentity;
    @Mock
    private ChangeUserStatusUseCase changeUserStatus;
    @Mock
    private ListUserRoleAssignmentsUseCase listUserRoleAssignments;
    @Mock
    private AssignRoleUseCase assignRole;
    @Mock
    private RevokeRoleUseCase revokeRole;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserAdminController controller = new UserAdminController(listUsers, getUserDetail, createLocalUser,
                updateLocalUserIdentity, changeUserStatus, listUserRoleAssignments, assignRole, revokeRole);
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

    private static User aUser() {
        return User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
    }

    @Test
    void list_returnsAPageOfUsers() throws Exception {
        User user = aUser();
        when(listUsers.handle(any(), any(), any())).thenReturn(new PageResult<>(List.of(user), 0, 20, 1));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("jdoe"));
    }

    @Test
    void create_returns201_withTheCreatedUser() throws Exception {
        User user = aUser();
        when(createLocalUser.handle(any(), any(), any())).thenReturn(new CreateLocalUserResult(user, "generated-pw"));

        mockMvc.perform(post("/auth/admin/users")
                        .contentType("application/json")
                        .content("{\"username\":\"jdoe\",\"email\":\"jdoe@example.com\",\"displayName\":\"Jane Doe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.generatedInitialPassword").value("generated-pw"));
    }

    @Test
    void create_duplicateUsername_returns409() throws Exception {
        when(createLocalUser.handle(any(), any(), any())).thenThrow(new UsernameAlreadyExistsException());

        mockMvc.perform(post("/auth/admin/users")
                        .contentType("application/json")
                        .content("{\"username\":\"jdoe\",\"email\":\"jdoe@example.com\",\"displayName\":\"Jane Doe\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("username-already-exists")));
    }

    @Test
    void insufficientPermission_returns403() throws Exception {
        when(listUsers.handle(any(), any(), any())).thenThrow(new InsufficientPermissionException());

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(containsString("insufficient-permissions")));
    }

    @Test
    void userNotFound_returns404() throws Exception {
        when(getUserDetail.handle(any(), any(), any())).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/auth/admin/users/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(containsString("user-not-found")));
    }

    @Test
    void disablingTheLastMasterAdmin_returns409() throws Exception {
        when(changeUserStatus.handle(any(), any(), any())).thenThrow(new MasterAdminContinuityViolationException());

        mockMvc.perform(post("/auth/admin/users/" + UUID.randomUUID() + "/disable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(containsString("master-admin-continuity-violation")));
    }
}
