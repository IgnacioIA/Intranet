package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

/** SPEC-AUTH-010 §12 RN-10: `PATCH /auth/admin/permissions/{name}`. {@code name} solo se acepta si coincide con el actual. */
public record UpdatePermissionRequest(String name, String description) {
}
