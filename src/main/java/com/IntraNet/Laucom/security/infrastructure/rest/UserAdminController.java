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
import com.IntraNet.Laucom.security.application.admin.UpdateLocalUserIdentityCommand;
import com.IntraNet.Laucom.security.application.admin.UpdateLocalUserIdentityUseCase;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.PageResult;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.AssignRoleRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateUserRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateUserResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.RoleAssignmentResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UpdateUserIdentityRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserDetailResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserStatusResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UserSummaryResponse;
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

/** SPEC-AUTH-010 §12 ("Usuarios" y "Asignación de Roles a Usuarios"). */
@RestController
@RequestMapping("/auth/admin/users")
public class UserAdminController {

    private final ListUsersUseCase listUsers;
    private final GetUserDetailUseCase getUserDetail;
    private final CreateLocalUserUseCase createLocalUser;
    private final UpdateLocalUserIdentityUseCase updateLocalUserIdentity;
    private final ChangeUserStatusUseCase changeUserStatus;
    private final ListUserRoleAssignmentsUseCase listUserRoleAssignments;
    private final AssignRoleUseCase assignRole;
    private final RevokeRoleUseCase revokeRole;

    public UserAdminController(ListUsersUseCase listUsers, GetUserDetailUseCase getUserDetail,
                                CreateLocalUserUseCase createLocalUser,
                                UpdateLocalUserIdentityUseCase updateLocalUserIdentity,
                                ChangeUserStatusUseCase changeUserStatus,
                                ListUserRoleAssignmentsUseCase listUserRoleAssignments,
                                AssignRoleUseCase assignRole, RevokeRoleUseCase revokeRole) {
        this.listUsers = listUsers;
        this.getUserDetail = getUserDetail;
        this.createLocalUser = createLocalUser;
        this.updateLocalUserIdentity = updateLocalUserIdentity;
        this.changeUserStatus = changeUserStatus;
        this.listUserRoleAssignments = listUserRoleAssignments;
        this.assignRole = assignRole;
        this.revokeRole = revokeRole;
    }

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

    @GetMapping("/{userId}")
    public UserDetailResponse detail(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        var user = getUserDetail.handle(AuthenticatedActor.resolve(), userId, RequestCorrelation.from(httpRequest));
        return UserDetailResponse.from(user);
    }

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

    @PatchMapping("/{userId}")
    public UserDetailResponse update(@PathVariable UUID userId, @RequestBody UpdateUserIdentityRequest request,
                                      HttpServletRequest httpRequest) {
        var user = updateLocalUserIdentity.handle(AuthenticatedActor.resolve(),
                new UpdateLocalUserIdentityCommand(userId, request.email(), request.displayName()),
                RequestCorrelation.from(httpRequest));
        return UserDetailResponse.from(user);
    }

    @PostMapping("/{userId}/enable")
    public UserStatusResponse enable(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.ENABLE, httpRequest);
    }

    @PostMapping("/{userId}/disable")
    public UserStatusResponse disable(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.DISABLE, httpRequest);
    }

    @PostMapping("/{userId}/lock")
    public UserStatusResponse lock(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.LOCK, httpRequest);
    }

    @PostMapping("/{userId}/unlock")
    public UserStatusResponse unlock(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return changeStatus(userId, AdminUserStatusOperation.UNLOCK, httpRequest);
    }

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

    @GetMapping("/{userId}/roles")
    public List<RoleAssignmentResponse> listRoles(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        return listUserRoleAssignments.handle(AuthenticatedActor.resolve(), userId, RequestCorrelation.from(httpRequest))
                .stream().map(RoleAssignmentResponse::from).toList();
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<RoleAssignmentResponse> assignRole(@PathVariable UUID userId,
                                                              @Valid @RequestBody AssignRoleRequest request,
                                                              HttpServletRequest httpRequest) {
        var assignment = assignRole.handle(AuthenticatedActor.resolve(),
                new AssignRoleCommand(userId, UUID.fromString(request.roleId())), RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleAssignmentResponse.from(assignment));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID userId, @PathVariable UUID roleId,
                                            HttpServletRequest httpRequest) {
        revokeRole.handle(AuthenticatedActor.resolve(), new RevokeRoleCommand(userId, roleId),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
