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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin — Roles", description = "SPEC-AUTH-010.")
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

    @Operation(summary = "Listar Roles", description = "SPEC-AUTH-010 §12. Requiere `ROLE_READ`. Catálogo acotado, sin paginación.")
    @ApiResponse(responseCode = "200", description = "Roles existentes.")
    @GetMapping
    public List<RoleResponse> list(HttpServletRequest httpRequest) {
        return listRoles.handle(AuthenticatedActor.resolve(), RequestCorrelation.from(httpRequest))
                .stream().map(RoleResponse::from).toList();
    }

    @Operation(summary = "Consultar un Role", description = "SPEC-AUTH-010 §12. Requiere `ROLE_READ`.")
    @ApiResponse(responseCode = "200", description = "Detalle del Role, incluidos sus permisos.")
    @ApiResponse(responseCode = "404", description = "Role inexistente (`role-not-found`).")
    @GetMapping("/{roleId}")
    public RoleResponse detail(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        return RoleResponse.from(getRole.handle(AuthenticatedActor.resolve(), roleId, RequestCorrelation.from(httpRequest)));
    }

    @Operation(summary = "Crear Role", description = "SPEC-AUTH-010 §12. Requiere `ROLE_MANAGE`. `permissions` es opcional (sin permisos si se omite).")
    @ApiResponse(responseCode = "201", description = "Role creado.")
    @ApiResponse(responseCode = "404", description = "Alguno de los `permissions` no existe (`permission-not-found`).")
    @ApiResponse(responseCode = "409", description = "Ya existe un Role con ese `name` (`role-already-exists`).")
    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateOrUpdateRoleRequest request,
                                                HttpServletRequest httpRequest) {
        var role = createRole.handle(AuthenticatedActor.resolve(),
                new CreateRoleCommand(request.name(), request.description(), request.permissions()),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @Operation(summary = "Modificar un Role", description = "SPEC-AUTH-010 §12. Requiere `ROLE_MANAGE`. "
            + "`name` en el cuerpo se ignora (identificado por `roleId` en la ruta); `permissions` reemplaza "
            + "el conjunto completo (calcula el diff internamente), `null` lo deja sin cambios.")
    @ApiResponse(responseCode = "200", description = "Role actualizado.")
    @ApiResponse(responseCode = "404", description = "Role o algún permiso inexistente.")
    @PutMapping("/{roleId}")
    public RoleResponse update(@PathVariable UUID roleId, @RequestBody CreateOrUpdateRoleRequest request,
                                HttpServletRequest httpRequest) {
        var role = updateRole.handle(AuthenticatedActor.resolve(),
                new UpdateRoleCommand(roleId, request.description(), request.permissions()),
                RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }

    @Operation(summary = "Activar Role", description = "SPEC-AUTH-010 §12. Requiere `ROLE_MANAGE`.")
    @ApiResponse(responseCode = "200", description = "Role activado.")
    @PostMapping("/{roleId}/activate")
    public RoleResponse activate(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        var role = setRoleActive.handle(AuthenticatedActor.resolve(), roleId, true, RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }

    @Operation(summary = "Desactivar Role", description = "SPEC-AUTH-010 §12. Requiere `ROLE_MANAGE`. "
            + "Pierde efecto inmediato en la siguiente request de cualquier usuario que lo tuviera (sin esperar "
            + "expiración del token), sin borrar las asignaciones (ADR-020).")
    @ApiResponse(responseCode = "200", description = "Role desactivado.")
    @ApiResponse(responseCode = "409", description = "Es un Role de sistema (`system-role-protected`, INV-AUTH-011).")
    @PostMapping("/{roleId}/deactivate")
    public RoleResponse deactivate(@PathVariable UUID roleId, HttpServletRequest httpRequest) {
        var role = setRoleActive.handle(AuthenticatedActor.resolve(), roleId, false, RequestCorrelation.from(httpRequest));
        return RoleResponse.from(role);
    }
}
