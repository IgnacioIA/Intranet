package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;

/** SPEC-AUTH-010 §12: `POST /auth/admin/users`. {@code initialPassword} es opcional. */
public record CreateUserRequest(@NotBlank String username, String email, String displayName, String initialPassword) {
}
