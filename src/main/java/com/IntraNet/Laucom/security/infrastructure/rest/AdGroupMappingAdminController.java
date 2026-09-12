package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.admin.CreateAdGroupMappingCommand;
import com.IntraNet.Laucom.security.application.admin.CreateAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.application.admin.DeleteAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.application.admin.ListAdGroupMappingsUseCase;
import com.IntraNet.Laucom.security.application.admin.UpdateAdGroupMappingCommand;
import com.IntraNet.Laucom.security.application.admin.UpdateAdGroupMappingUseCase;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.AdGroupRoleMappingResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.admin.CreateOrUpdateAdGroupMappingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SPEC-AUTH-008 §12 ("Administración del mapping AD Group -> Role"). */
@RestController
@RequestMapping("/auth/admin/ad-group-mappings")
@Tag(name = "Admin — AD Group Mappings", description = "SPEC-AUTH-008. Un AD Group no mapeado no concede ningún Role.")
public class AdGroupMappingAdminController {

    private final ListAdGroupMappingsUseCase listMappings;
    private final CreateAdGroupMappingUseCase createMapping;
    private final UpdateAdGroupMappingUseCase updateMapping;
    private final DeleteAdGroupMappingUseCase deleteMapping;

    public AdGroupMappingAdminController(ListAdGroupMappingsUseCase listMappings,
                                          CreateAdGroupMappingUseCase createMapping,
                                          UpdateAdGroupMappingUseCase updateMapping,
                                          DeleteAdGroupMappingUseCase deleteMapping) {
        this.listMappings = listMappings;
        this.createMapping = createMapping;
        this.updateMapping = updateMapping;
        this.deleteMapping = deleteMapping;
    }

    @Operation(summary = "Listar mappings AD Group -> Role", description = "SPEC-AUTH-008 §12. Requiere `AD_MAPPING_MANAGE`.")
    @ApiResponse(responseCode = "200", description = "Mappings existentes.")
    @GetMapping
    public List<AdGroupRoleMappingResponse> list(HttpServletRequest httpRequest) {
        return listMappings.handle(AuthenticatedActor.resolve(), RequestCorrelation.from(httpRequest))
                .stream().map(AdGroupRoleMappingResponse::from).toList();
    }

    @Operation(summary = "Crear mapping AD Group -> Role", description = "SPEC-AUTH-008 §12, RN-01/02/05. "
            + "Requiere `AD_MAPPING_MANAGE`. `adGroupIdentifier` debe ser único; `roleId` debe existir y "
            + "estar activo.")
    @ApiResponse(responseCode = "201", description = "Mapping creado.")
    @ApiResponse(responseCode = "400", description = "El Role referenciado está inactivo (`role-inactive`).")
    @ApiResponse(responseCode = "404", description = "El Role referenciado no existe (`role-not-found`).")
    @ApiResponse(responseCode = "409", description = "Ya existe un mapping para ese grupo (`group-already-mapped`).")
    @PostMapping
    public ResponseEntity<AdGroupRoleMappingResponse> create(@Valid @RequestBody CreateOrUpdateAdGroupMappingRequest request,
                                                              HttpServletRequest httpRequest) {
        var mapping = createMapping.handle(AuthenticatedActor.resolve(),
                new CreateAdGroupMappingCommand(request.adGroupIdentifier(), UUID.fromString(request.roleId())),
                RequestCorrelation.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(AdGroupRoleMappingResponse.from(mapping));
    }

    @Operation(summary = "Modificar mapping AD Group -> Role", description = "SPEC-AUTH-008 §12, RN-02/05. "
            + "Requiere `AD_MAPPING_MANAGE`. Permite cambiar tanto `adGroupIdentifier` como `roleId`; la "
            + "unicidad de `adGroupIdentifier` también aplica aquí. El efecto sobre usuarios ya provisionados "
            + "se aplica en su siguiente login AD, no de inmediato (RN-04).")
    @ApiResponse(responseCode = "200", description = "Mapping actualizado.")
    @ApiResponse(responseCode = "400", description = "El nuevo Role está inactivo (`role-inactive`).")
    @ApiResponse(responseCode = "404", description = "Mapping o Role inexistente (`mapping-not-found`/`role-not-found`).")
    @ApiResponse(responseCode = "409", description = "El nuevo `adGroupIdentifier` ya pertenece a otro mapping (`group-already-mapped`).")
    @PutMapping("/{id}")
    public AdGroupRoleMappingResponse update(@PathVariable UUID id,
                                              @Valid @RequestBody CreateOrUpdateAdGroupMappingRequest request,
                                              HttpServletRequest httpRequest) {
        var mapping = updateMapping.handle(AuthenticatedActor.resolve(),
                new UpdateAdGroupMappingCommand(id, request.adGroupIdentifier(), UUID.fromString(request.roleId())),
                RequestCorrelation.from(httpRequest));
        return AdGroupRoleMappingResponse.from(mapping);
    }

    @Operation(summary = "Eliminar mapping AD Group -> Role", description = "SPEC-AUTH-008 §12, RN-04. "
            + "Requiere `AD_MAPPING_MANAGE`. No recorre usuarios existentes: los roles ya derivados de este "
            + "mapping se recalculan recién en el siguiente login AD de cada usuario afectado.")
    @ApiResponse(responseCode = "204", description = "Mapping eliminado.")
    @ApiResponse(responseCode = "404", description = "Mapping inexistente (`mapping-not-found`).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        deleteMapping.handle(AuthenticatedActor.resolve(), id, RequestCorrelation.from(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
