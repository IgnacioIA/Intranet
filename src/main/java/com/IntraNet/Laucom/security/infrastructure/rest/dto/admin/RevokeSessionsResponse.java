package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

/** SPEC-AUTH-004 §12: `POST /auth/admin/users/{userId}/revoke-sessions`. */
public record RevokeSessionsResponse(String status, int revokedSessions) {

    public static RevokeSessionsResponse of(int revokedSessions) {
        return new RevokeSessionsResponse("ok", revokedSessions);
    }
}
