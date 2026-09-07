package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UC-AUTH-015 SPEC-AUTH-010: `GET /auth/admin/users/{userId}`, requiere {@code USER_READ}. */
@Service
public class GetUserDetailUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;

    public GetUserDetailUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
    }

    public User handle(UUID actorId, UUID targetUserId, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_READ, correlationId);
        return userRepository.findById(targetUserId).orElseThrow(UserNotFoundException::new);
    }
}
