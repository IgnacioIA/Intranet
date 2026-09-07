package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;

import java.util.Set;
import java.util.stream.Collectors;

/** SPEC-AUTH-010 §12: Roles. */
public record RoleResponse(String id, String name, String description, boolean isSystemRole, boolean active,
                            Set<String> permissions) {

    public static RoleResponse from(Role role) {
        Set<String> permissionNames = role.permissions().stream()
                .map(Permission::name)
                .collect(Collectors.toUnmodifiableSet());
        return new RoleResponse(role.id().toString(), role.name(), role.description(), role.isSystemRole(),
                role.isActive(), permissionNames);
    }
}
