package com.IntraNet.Laucom.security.application.admin;

import java.util.Set;
import java.util.UUID;

/** UC-AUTH-019 SPEC-AUTH-010 (modificación): reemplaza description y el conjunto de Permission. */
public record UpdateRoleCommand(UUID roleId, String description, Set<String> permissionNames) {
}
