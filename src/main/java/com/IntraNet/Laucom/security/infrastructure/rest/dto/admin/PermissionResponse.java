package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.Permission;

/**
 * SPEC-AUTH-010 §12: Permissions. {@code id} y {@code name} son el mismo valor: el dominio
 * identifica a {@code Permission} únicamente por su {@code name} (Value Object, entities.md), no
 * existe un identificador sustituto separado — a diferencia del ejemplo de la SPEC, que sugiere
 * un slug distinto sin que exista un atributo real que lo respalde.
 */
public record PermissionResponse(String id, String name, String description, boolean isSystemPermission,
                                  boolean active) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.name(), permission.name(), permission.description(),
                permission.isSystemPermission(), permission.isActive());
    }
}
