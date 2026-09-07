package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.User;

import java.util.Set;
import java.util.stream.Collectors;

/** SPEC-AUTH-010 §12: `GET /auth/admin/users/{userId}` — incluye roles con procedencia. */
public record UserDetailResponse(String id, String provider, String username, String displayName, String email,
                                  String status, Set<RoleAssignmentResponse> roles) {

    public static UserDetailResponse from(User user) {
        Set<RoleAssignmentResponse> roles = user.roles().stream()
                .map(RoleAssignmentResponse::from)
                .collect(Collectors.toUnmodifiableSet());
        return new UserDetailResponse(user.id().toString(), user.provider().name(), user.username(),
                user.displayName(), user.email().orElse(null), user.status().name(), roles);
    }
}
