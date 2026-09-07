package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-022 SPEC-AUTH-010. */
public record RevokeRoleCommand(UUID userId, UUID roleId) {
}
