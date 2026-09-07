package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;

/** SPEC-AUTH-010 §12: `POST /auth/admin/permissions`. */
public record CreatePermissionRequest(@NotBlank String name, String description) {
}
