package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.User;

/** SPEC-AUTH-010 §12: respuesta común a `.../enable`, `.../disable`, `.../lock`, `.../unlock`, `.../deprovision`. */
public record UserStatusResponse(String id, String status) {

    public static UserStatusResponse from(User user) {
        return new UserStatusResponse(user.id().toString(), user.status().name());
    }
}
