package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

/** SPEC-AUTH-010 §12: `PATCH /auth/admin/users/{userId}`. */
public record UpdateUserIdentityRequest(String email, String displayName) {
}
