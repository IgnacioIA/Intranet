package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.User;

/** SPEC-AUTH-010 §12: fila de `GET /auth/admin/users` — sin datos sensibles. */
public record UserSummaryResponse(String id, String provider, String username, String displayName, String email,
                                   String status) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.id().toString(), user.provider().name(), user.username(),
                user.displayName(), user.email().orElse(null), user.status().name());
    }
}
