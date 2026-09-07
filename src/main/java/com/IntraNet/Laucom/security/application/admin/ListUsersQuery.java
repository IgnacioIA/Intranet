package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;

/** UC-AUTH-015 SPEC-AUTH-010: `GET /auth/admin/users`. */
public record ListUsersQuery(UserStatus status, IdentityProvider provider, String searchText, int page, int size) {
}
