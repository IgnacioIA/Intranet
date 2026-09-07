package com.IntraNet.Laucom.security.infrastructure.rest.dto;

import com.IntraNet.Laucom.security.application.identity.CurrentUserView;

import java.util.Set;

/** SPEC-AUTH-005 §12. */
public record CurrentUserResponse(String id, String provider, String username, String displayName, String email,
                                   String status, Set<String> roles, Set<String> permissions) {

    public static CurrentUserResponse from(CurrentUserView view) {
        return new CurrentUserResponse(view.id().toString(), view.provider().name(), view.username(),
                view.displayName(), view.email(), view.status().name(), view.roles(), view.permissions());
    }
}
