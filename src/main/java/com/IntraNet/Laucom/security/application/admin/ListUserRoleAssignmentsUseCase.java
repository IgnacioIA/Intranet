package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/** SPEC-AUTH-010 §12: `GET /auth/admin/users/{userId}/roles`, requiere {@code USER_READ}. */
@Service
public class ListUserRoleAssignmentsUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;

    public ListUserRoleAssignmentsUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
    }

    public Set<UserRoleAssignment> handle(UUID actorId, UUID targetUserId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_READ, correlationId);
        User user = userRepository.findById(targetUserId).orElseThrow(UserNotFoundException::new);
        return user.roles();
    }
}
