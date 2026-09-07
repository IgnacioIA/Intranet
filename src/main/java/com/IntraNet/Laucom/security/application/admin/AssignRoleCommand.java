package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-021 SPEC-AUTH-010. */
public record AssignRoleCommand(UUID userId, UUID roleId) {
}
