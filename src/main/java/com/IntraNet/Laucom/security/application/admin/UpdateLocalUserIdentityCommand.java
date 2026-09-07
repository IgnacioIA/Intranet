package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-017 SPEC-AUTH-010. */
public record UpdateLocalUserIdentityCommand(UUID userId, String email, String displayName) {
}
