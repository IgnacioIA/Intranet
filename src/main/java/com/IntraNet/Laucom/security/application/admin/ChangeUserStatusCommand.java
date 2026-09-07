package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-018 SPEC-AUTH-010. */
public record ChangeUserStatusCommand(UUID userId, AdminUserStatusOperation operation) {
}
