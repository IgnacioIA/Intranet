package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreateRoleCommand;
import com.IntraNet.Laucom.security.application.admin.CreateRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.GetRoleUseCase;
import com.IntraNet.Laucom.security.application.admin.ListRolesUseCase;
import com.IntraNet.Laucom.security.application.admin.SetRoleActiveUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateRoleCommand;
import com.IntraNet.Laucom.security.application.admin.UpdateRoleUseCase;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateOrUpdateRoleRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.RoleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SPEC-AUTH-010 §12 ("Roles"). */
@RestController
@RequestMapping("/auth/admin/roles")
public class RoleAdminController {

    private final ListRolesUseCase listRoles;
    private final GetRoleUseCase getRole;
    private final CreateRoleUseCase createRole;
    private final UpdateRoleUseCase updateRole;
    private final SetRoleActiveUseCase setRoleActive;

    public RoleAdminController(ListRolesUseCase listRoles, GetRoleUseCase getRole, CreateRoleUseCase createRole,
                                UpdateRoleUseCase updateRole, SetRoleActiveUseCase setRoleActive) {
        this.listRoles = listRoles;
        this.getRole = getRole;
        this.createRole = createRole;
        this.updateRole = updateRole;
        this.setRoleActive = setRoleActive;
    }

    @GetMapping
    public List<RoleResponse> list(HttpServletRequest httpRequest) {
        return listRoles.handle(AuthenticatedActor.resolve(), RequestCorrelation.from(httpRequest))
                .stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/{roleId}")
    public RoleResponse detail(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        return RoleResponse.from(getRole.handle(AuthenticatedActor.resolve(), roleId, RequestCorrelation.from(httpRequest)));
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateOrUpdateRoleRequest request,
                                                HttpServletRequest httpRequest) {
        var role = createRole.handle(AuthenticatedActor.resolve(),
                new CreateRoleCommand(request.name(), request.description(), request.permissions()),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @PutMapping("/{roleId}")
    public RoleResponse update(@PathVariable UUID roleId, @RequestBody CreateOrUpdateRoleRequest request,
                                HttpServletRequest httpRequest) {
        var role = updateRole.handle(AuthenticatedActor.resolve(),
                new UpdateRoleCommand(roleId, request.description(), request.permissions()),
                RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }

    @PostMapping("/{roleId}/activate")
    public RoleResponse activate(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        var role = setRoleActive.handle(AuthenticatedActor.resolve(), roleId, true, RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }

    @PostMapping("/{roleId}/deactivate")
    public RoleResponse deactivate(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        var role = setRoleActive.handle(AuthenticatedActor.resolve(), roleId, false, RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }
}
