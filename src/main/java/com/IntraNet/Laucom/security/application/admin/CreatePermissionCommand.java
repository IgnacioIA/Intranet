package com.IntraNet.Laucom.security.application.admin;

/** UC-AUTH-020 SPEC-AUTH-010 (alta). {@code name} sigue la convención RECURSO_ACCION. */
public record CreatePermissionCommand(String name, String description) {
}
