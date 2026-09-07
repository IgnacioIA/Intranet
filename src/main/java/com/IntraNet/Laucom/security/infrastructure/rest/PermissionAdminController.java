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

    @GetMapping
    public List<PermissionResponse> list(HttpServletRequest httpRequest) {
        return listPermissions.handle(AuthenticatedActor.resolve(), RequestCorrelation.from(httpRequest))
                .stream().map(PermissionResponse::from).toList();
    }

    @GetMapping("/{name}")
    public PermissionResponse detail(@PathVariable String name, HttpServletRequest httpRequest) {
        return PermissionResponse.from(
                getPermission.handle(AuthenticatedActor.resolve(), name, RequestCorrelation.from(httpRequest)));
    }

    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request,
                                                      HttpServletRequest httpRequest) {
        var permission = createPermission.handle(AuthenticatedActor.resolve(),
                new CreatePermissionCommand(request.name(), request.description()), RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionResponse.from(permission));
    }

    @PatchMapping("/{name}")
    public PermissionResponse update(@PathVariable String name, @RequestBody UpdatePermissionRequest request,
                                      HttpServletRequest httpRequest) {
        var permission = updatePermissionDescription.handle(AuthenticatedActor.resolve(),
                new UpdatePermissionDescriptionCommand(name, request.name(), request.description()),
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }

    @PostMapping("/{name}/activate")
    public PermissionResponse activate(@PathVariable String name, HttpServletRequest httpRequest) {
        var permission = setPermissionActive.handle(AuthenticatedActor.resolve(), name, true,
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }

    @PostMapping("/{name}/deactivate")
    public PermissionResponse deactivate(@PathVariable String name, HttpServletRequest httpRequest) {
        var permission = setPermissionActive.handle(AuthenticatedActor.resolve(), name, false,
                RequestCorrelation.from(httpRequest));
        return PermissionResponse.from(permission);
    }
}
