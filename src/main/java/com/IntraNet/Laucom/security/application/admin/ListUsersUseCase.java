package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.port.PageResult;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserSearchCriteria;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UC-AUTH-015 SPEC-AUTH-010: `GET /auth/admin/users`, requiere {@code USER_READ}. */
@Service
public class ListUsersUseCase {

    private final AdminActionAuthorizer adminActionAuthorizer;
    private final UserRepositoryPort userRepository;

    public ListUsersUseCase(AdminActionAuthorizer adminActionAuthorizer, UserRepositoryPort userRepository) {
        this.adminActionAuthorizer = adminActionAuthorizer;
        this.userRepository = userRepository;
    }

    public PageResult<User> handle(UUID actorId, ListUsersQuery query, String correlationId) {
        adminActionAuthorizer.require(actorId, WellKnownPermissions.USER_READ, correlationId);
        return userRepository.search(new UserSearchCriteria(
                query.status(), query.provider(), query.searchText(), query.page(), query.size()));
    }
}
