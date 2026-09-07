package com.IntraNet.Laucom.security.application.identity;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;

import java.util.Set;
import java.util.UUID;

/** UC-AUTH-009 SPEC-AUTH-005 §12: representación completa de la identidad/autorización actual. */
public record CurrentUserView(UUID id, IdentityProvider provider, String username, String displayName, String email,
                               UserStatus status, Set<String> roles, Set<String> permissions) {
}
