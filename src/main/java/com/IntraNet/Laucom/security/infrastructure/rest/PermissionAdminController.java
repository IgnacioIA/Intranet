package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreatePermissionCommand;
import com.IntraNet.Laucom.security.application.admin.CreatePermissionUseCase;
import com.IntraNet.Laucom.security.application.admin.GetPermissionUseCase;
import com.IntraNet.Laucom.security.application.admin.ListPermissionsUseCase;
import com.IntraNet.Laucom.security.application.admin.SetPermissionActiveUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdatePermissionDescriptionCommand;
import com.IntraNet.Laucom.security.application.admin.UpdatePermissionDescriptionUseCase;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreatePermissionRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.PermissionResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.UpdatePermissionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SPEC-AUTH-010 §12 ("Permissions"). Identificadas por {@code name} (ver {@code PermissionResponse}). */
@RestController
@RequestMapping("/auth/admin/permissions")
@Tag(name = "Admin — Permissions", description = "SPEC-AUTH-010. Ver también ADR-021 (Permission Catalog): "
        + "los permisos de aplicaciones consumidoras se declaran por código, no se crean arbitrariamente aquí.")
public class PermissionAdminController {

    private final ListPermissionsUseCase listPermissions;
    private final GetPermissionUseCase getPermission;
    private final CreatePermissionUseCase createPermission;
    private final UpdatePermissionDescriptionUseCase updatePermissionDescription;
    private final SetPermissionActiveUseCase setPermissionActive;

    public PermissionAdminController(ListPermissionsUseCase listPermissions, GetPermissionUseCase getPermission,
                                      CreatePermissionUseCase createPermission,
                                      UpdatePermissionDescriptionUseCase updatePermissionDescription,
                                      SetPermissionActiveUseCase setPermissionActive) {
        this.listPermissions = listPermissions;
        this.getPermission = getPermission;
        this.createPermission = createPermission;
        this.updatePermissionDescription = updatePermissionDescription;
        this.setPermissionActive = setPermissionActive;
    }

    @Operation(summary = "Listar Permissions", description = "SPEC-AUTH-010 §12. Requiere `PERMISSION_READ`.")
    @ApiResponse(responseCode = "200", description = "Permissions existentes.")
    @GetMapping
    public List<PermissionResponse> list(HttpServletRequest httpRequest) {
        return listPermissions.handle(AuthenticatedActor.resolve(), RequestCorrelation.from(httpRequest))
                .stream().map(PermissionResponse::from).toList();
    }

    @Operation(summary = "Consultar una Permission", description = "SPEC-AUTH-010 §12. Requiere `PERMISSION_READ`. `id` y `name` son el mismo valor.")
    @ApiResponse(responseCode = "200", description = "Detalle de la Permission.")
    @ApiResponse(responseCode = "404", description = "Permission inexistente (`permission-not-found`).")
    @GetMapping("/{name}")
    public PermissionResponse detail(@PathVariable String name, HttpServletRequest httpRequest) {
        return PermissionResponse.from(
                getPermission.handle(AuthenticatedActor.resolve(), name, RequestCorrelation.from(httpRequest)));
    }

    @Operation(summary = "Crear Permission", description = "SPEC-AUTH-010 §12. Requiere `PERMISSION_MANAGE`. "
            + "`name` sigue la convención `RECURSO_ACCION`. Para permisos de una aplicación consumidora, "
            + "preferir declararlos vía `PermissionCatalog` (ADR-021) en vez de crearlos aquí uno por uno.")
    @ApiResponse(responseCode = "201", description = "Permission creada.")
    @ApiResponse(responseCode = "409", description = "Ya existe una Permission con ese `name` (`permission-already-exists`).")
    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request,
                                                      HttpServletRequest httpRequest) {
        var permission = createPermission.handle(AuthenticatedActor.resolve(),
                new CreatePermissionCommand(request.name(), request.description()), RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionResponse.from(permission));
    }

    @Operation(summary = "Modificar descripción de una Permission", description = "SPEC-AUTH-010 §12, RN-10. "
            + "Requiere `PERMISSION_MANAGE`. Solo `description` es editable; `name` es inmutable — si se "
            + "envía y difiere del actual, se rechaza.")
    @ApiResponse(responseCode = "200", description = "Descripción actualizada.")
    @ApiResponse(responseCode = "400", description = "Se intentó cambiar `name` (`permission-name-immutable`).")
    @ApiResponse(responseCode = "404", description = "Permission inexistente (`permission-not-found`).")
    @PatchMapping("/{name}")
    public PermissionResponse update(@PathVariable String name, @RequestBody UpdatePermissionRequest request,
                                      HttpServletRequest httpRequest) {
        var permission = updatePermissionDescription.handle(AuthenticatedActor.resolve(),
                new UpdatePermissionDescriptionCommand(name, request.name(), request.description()),
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }

    @Operation(summary = "Activar Permission", description = "SPEC-AUTH-010 §12. Requiere `PERMISSION_MANAGE`.")
    @ApiResponse(responseCode = "200", description = "Permission activada.")
    @PostMapping("/{name}/activate")
    public PermissionResponse activate(@PathVariable String name, HttpServletRequest httpRequest) {
        var permission = setPermissionActive.handle(AuthenticatedActor.resolve(), name, true,
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }

    @Operation(summary = "Desactivar Permission", description = "SPEC-AUTH-010 §12. Requiere `PERMISSION_MANAGE`. "
            + "Pierde efecto inmediato en la siguiente request de autorización (sin esperar expiración del token).")
    @ApiResponse(responseCode = "200", description = "Permission desactivada.")
    @ApiResponse(responseCode = "409", description = "Es una Permission de sistema (`system-permission-protected`).")
    @PostMapping("/{name}/deactivate")
    public PermissionResponse deactivate(@PathVariable String name, HttpServletRequest httpRequest) {
        var permission = setPermissionActive.handle(AuthenticatedActor.resolve(), name, false,
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }
}
