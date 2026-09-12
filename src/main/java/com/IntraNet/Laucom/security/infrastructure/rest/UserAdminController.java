package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.AdminUserStatusOperation;
import com.IntraNet.Laucom.security.application.admin.AssignRoleCommand;
import com.IntraNet.Laucom.security.application.admin.AssignRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.ChangeUserStatusCommand;
import com.IntraNet.Laucom.security.application.admin.ChangeUserStatusUseCase;
import com.IntraNet.Laucom.security.application.admin.CreateLocalUserCommand;
import com.IntraNet.Laucom.security.application.admin.CreateLocalUserUseCase;
import com.IntraNet.Laucom.security.application.admin.GetUserDetailUseCase;
import com.IntraNet.Laucom.security.application.admin.ListUserRoleAssignmentsUseCase;
import com.IntraNet.Laucom.security.application.admin.ListUsersQuery;
import com.IntraNet.Laucom.security.application.admin.ListUsersUseCase;
import com.IntraNet.Laucom.security.application.admin.RevokeRoleCommand;
import com.IntraNet.Laucom.security.application.admin.RevokeRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.RevokeUserSessionsUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateLocalUserIdentityCommand;
import com.IntraNet.Laucom.security.application.admin.UpdateLocalUserIdentityUseCase;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.PageResult;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.AssignRoleRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateUserRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateUserResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.RevokeSessionsResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.RoleAssignmentResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UpdateUserIdentityRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserDetailResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserStatusResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SPEC-AUTH-010 §12 ("Usuarios" y "Asignación de Roles a Usuarios"), SPEC-AUTH-004 §12. */
@RestController
@RequestMapping("/auth/admin/users")
@Tag(name = "Admin — Users", description = "SPEC-AUTH-010 (Usuarios, asignación de Roles), SPEC-AUTH-004 (revocación de sesiones).")
public class UserAdminController {

    private final ListUsersUseCase listUsers;
    private final GetUserDetailUseCase getUserDetail;
    private final CreateLocalUserUseCase createLocalUser;
    private final UpdateLocalUserIdentityUseCase updateLocalUserIdentity;
    private final ChangeUserStatusUseCase changeUserStatus;
    private final ListUserRoleAssignmentsUseCase listUserRoleAssignments;
    private final AssignRoleUseCase assignRole;
    private final RevokeRoleUseCase revokeRole;
    private final RevokeUserSessionsUseCase revokeUserSessions;

    public UserAdminController(ListUsersUseCase listUsers, GetUserDetailUseCase getUserDetail,
                                CreateLocalUserUseCase createLocalUser,
                                UpdateLocalUserIdentityUseCase updateLocalUserIdentity,
                                ChangeUserStatusUseCase changeUserStatus,
                                ListUserRoleAssignmentsUseCase listUserRoleAssignments,
                                AssignRoleUseCase assignRole, RevokeRoleUseCase revokeRole,
                                RevokeUserSessionsUseCase revokeUserSessions) {
        this.listUsers = listUsers;
        this.getUserDetail = getUserDetail;
        this.createLocalUser = createLocalUser;
        this.updateLocalUserIdentity = updateLocalUserIdentity;
        this.changeUserStatus = changeUserStatus;
        this.listUserRoleAssignments = listUserRoleAssignments;
        this.assignRole = assignRole;
        this.revokeRole = revokeRole;
        this.revokeUserSessions = revokeUserSessions;
    }

    @Operation(summary = "Listar usuarios", description = "SPEC-AUTH-010 §12. Requiere `USER_READ`. Filtros opcionales + paginación.")
    @ApiResponse(responseCode = "200", description = "Página de usuarios.")
    @ApiResponse(responseCode = "403", description = "Sin `USER_READ` (`insufficient-permissions`).")
    @GetMapping
    public List<UserSummaryResponse> list(@RequestParam(required = false) UserStatus status,
                                           @RequestParam(required = false) IdentityProvider provider,
                                           @RequestParam(name = "q", required = false) String searchText,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           HttpServletRequest httpRequest) {
        PageResult<com.IntraNet.Laucom.security.domain.model.User> result = listUsers.handle(
                AuthenticatedActor.resolve(), new ListUsersQuery(status, provider, searchText, page, size),
                RequestCorrelation.from(httpRequest));
        return result.items().stream().map(UserSummaryResponse::from).toList();
    }

    @Operation(summary = "Consultar un usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_READ`.")
    @ApiResponse(responseCode = "200", description = "Detalle del usuario.")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente (`user-not-found`).")
    @GetMapping("/{userId}")
    public UserDetailResponse detail(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        var user = getUserDetail.handle(AuthenticatedActor.resolve(), userId, RequestCorrelation.from(httpRequest));
        return UserDetailResponse.from(user);
    }

    @Operation(summary = "Crear usuario LOCAL", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. "
            + "`initialPassword` es opcional: si se omite, se genera una y se devuelve una única vez en la respuesta.")
    @ApiResponse(responseCode = "201", description = "Usuario creado.")
    @ApiResponse(responseCode = "409", description = "`username` ya existe (`username-already-exists`).")
    @ApiResponse(responseCode = "422", description = "Contraseña provista que no cumple la política (`password-policy-violation`).")
    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                                      HttpServletRequest httpRequest) {
        char[] initialPassword = (request.initialPassword() == null || request.initialPassword().isBlank())
                ? null : request.initialPassword().toCharArray();
        var result = createLocalUser.handle(AuthenticatedActor.resolve(),
                new CreateLocalUserCommand(request.username(), request.email(), request.displayName(), initialPassword),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateUserResponse.from(result));
    }

    @Operation(summary = "Actualizar identidad de un usuario LOCAL", description = "SPEC-AUTH-010 §12. "
            + "Requiere `USER_MANAGE`. Rechaza usuarios `ACTIVE_DIRECTORY` (la identidad la gestiona AD, RN-02).")
    @ApiResponse(responseCode = "200", description = "Identidad actualizada.")
    @ApiResponse(responseCode = "400", description = "Usuario `ACTIVE_DIRECTORY` (`identity-managed-externally`).")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente (`user-not-found`).")
    @PatchMapping("/{userId}")
    public UserDetailResponse update(@PathVariable UUID userId, @RequestBody UpdateUserIdentityRequest request,
                                      HttpServletRequest httpRequest) {
        var user = updateLocalUserIdentity.handle(AuthenticatedActor.resolve(),
                new UpdateLocalUserIdentityCommand(userId, request.email(), request.displayName()),
                RequestCorrelation.from(httpRequest));
        return UserDetailResponse.from(user);
    }

    @Operation(summary = "Habilitar usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. Re-evalúa roles (no asume `ACTIVE` ciegamente, RN-05).")
    @ApiResponse(responseCode = "200", description = "Nuevo estado del usuario.")
    @ApiResponse(responseCode = "400", description = "Transición inválida desde el estado actual (`invalid-state-transition`).")
    @PostMapping("/{userId}/enable")
    public UserStatusResponse enable(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.ENABLE, httpRequest);
    }

    @Operation(summary = "Deshabilitar usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. Revoca todas sus sesiones.")
    @ApiResponse(responseCode = "200", description = "Nuevo estado del usuario.")
    @ApiResponse(responseCode = "409", description = "Dejaría sin ningún `MASTER_ADMIN` activo (`master-admin-continuity-violation`, INV-AUTH-013).")
    @PostMapping("/{userId}/disable")
    public UserStatusResponse disable(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.DISABLE, httpRequest);
    }

    @Operation(summary = "Bloquear usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. Bloqueo administrativo manual, sin cooldown automático asociado.")
    @ApiResponse(responseCode = "200", description = "Nuevo estado del usuario.")
    @ApiResponse(responseCode = "409", description = "Dejaría sin ningún `MASTER_ADMIN` activo (`master-admin-continuity-violation`).")
    @PostMapping("/{userId}/lock")
    public UserStatusResponse lock(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.LOCK, httpRequest);
    }

    @Operation(summary = "Desbloquear usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. No verifica continuidad de MASTER_ADMIN (no aplica al desbloqueo) ni revoca sesiones.")
    @ApiResponse(responseCode = "200", description = "Nuevo estado del usuario.")
    @PostMapping("/{userId}/unlock")
    public UserStatusResponse unlock(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.UNLOCK, httpRequest);
    }

    @Operation(summary = "Deprovisionar usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_MANAGE`. Transición terminal (RN-06), irreversible.")
    @ApiResponse(responseCode = "200", description = "Nuevo estado del usuario.")
    @ApiResponse(responseCode = "409", description = "Dejaría sin ningún `MASTER_ADMIN` activo (`master-admin-continuity-violation`).")
    @PostMapping("/{userId}/deprovision")
    public UserStatusResponse deprovision(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.DEPROVISION, httpRequest);
    }

    private UserStatusResponse changeStatus(UUID userId, AdminUserStatusOperation operation,
                                             HttpServletRequest httpRequest) {
        var user = changeUserStatus.handle(AuthenticatedActor.resolve(),
                new ChangeUserStatusCommand(userId, operation), RequestCorrelation.from(httpRequest));
        return UserStatusResponse.from(user);
    }

    @Operation(summary = "Revocar todas las sesiones de un usuario", description = "SPEC-AUTH-004 §12. "
            + "Requiere `SESSION_REVOKE_ANY`. Revoca todas las familias de Refresh Token activas; los "
            + "Access Tokens ya emitidos NO se invalidan (expiran naturalmente). Idempotente: `revokedSessions` "
            + "es la cantidad de familias revocadas por esta llamada, 0 si no había ninguna activa.")
    @ApiResponse(responseCode = "200", description = "Revocación ejecutada (o no había nada que revocar).")
    @ApiResponse(responseCode = "403", description = "Sin `SESSION_REVOKE_ANY` (`insufficient-permissions`).")
    @ApiResponse(responseCode = "404", description = "Usuario objetivo inexistente (`user-not-found`).")
    @PostMapping("/{userId}/revoke-sessions")
    public RevokeSessionsResponse revokeSessions(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        int revokedSessions = revokeUserSessions.handle(AuthenticatedActor.resolve(), userId,
                RequestCorrelation.from(httpRequest));
        return RevokeSessionsResponse.of(revokedSessions);
    }

    @Operation(summary = "Listar asignaciones de Role de un usuario", description = "SPEC-AUTH-010 §12. Requiere `USER_READ`. Incluye tanto `GRANTED_EXPLICITLY` como `DERIVED_FROM_AD`.")
    @ApiResponse(responseCode = "200", description = "Asignaciones vigentes.")
    @GetMapping("/{userId}/roles")
    public List<RoleAssignmentResponse> listRoles(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return listUserRoleAssignments.handle(AuthenticatedActor.resolve(), userId, RequestCorrelation.from(httpRequest))
                .stream().map(RoleAssignmentResponse::from).toList();
    }

    @Operation(summary = "Asignar Role a un usuario", description = "SPEC-AUTH-010 §12. Requiere `ROLE_ASSIGN`. "
            + "Idempotente sobre una asignación `GRANTED_EXPLICITLY` ya existente; hace *upgrade* de una "
            + "asignación `DERIVED_FROM_AD` existente sin duplicar fila (INV-AUTH-015).")
    @ApiResponse(responseCode = "201", description = "Asignación creada (o ya existente).")
    @ApiResponse(responseCode = "400", description = "El Role está inactivo (`role-inactive`).")
    @ApiResponse(responseCode = "404", description = "Usuario o Role inexistente (`user-not-found`/`role-not-found`).")
    @PostMapping("/{userId}/roles")
    public ResponseEntity<RoleAssignmentResponse> assignRole(@PathVariable UUID userId,
                                                              @Valid @RequestBody AssignRoleRequest request,
                                                              HttpServletRequest httpRequest) {
        var assignment = assignRole.handle(AuthenticatedActor.resolve(),
                new AssignRoleCommand(userId, UUID.fromString(request.roleId())), RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleAssignmentResponse.from(assignment));
    }

    @Operation(summary = "Revocar una asignación explícita de Role", description = "SPEC-AUTH-010 §12. "
            + "Requiere `ROLE_REVOKE`. Solo revoca asignaciones `GRANTED_EXPLICITLY`; una `DERIVED_FROM_AD` "
            + "pura se rechaza orientando a SPEC-AUTH-008 (`assignment-not-explicit`).")
    @ApiResponse(responseCode = "204", description = "Asignación revocada.")
    @ApiResponse(responseCode = "409", description = "La asignación es puramente `DERIVED_FROM_AD` (`assignment-not-explicit`) o dejaría sin ningún `MASTER_ADMIN` (`master-admin-continuity-violation`).")
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID userId, @PathVariable UUID roleId,
                                            HttpServletRequest httpRequest) {
        revokeRole.handle(AuthenticatedActor.resolve(), new RevokeRoleCommand(userId, roleId),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
