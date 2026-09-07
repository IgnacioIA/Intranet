package com.IntraNet.Laucom.security.application.identity;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC-AUTH-009 SPEC-AUTH-005: identidad y autorización vigentes del usuario autenticado. RN-01:
 * refleja el estado en base de datos en el momento de la consulta (misma fuente de verdad que
 * SPEC-AUTH-006) — nunca deriva roles/permisos del contenido del Access Token.
 *
 * <p>No pasa por {@code AuthorizationService}: SPEC-AUTH-005 §12 exige únicamente "estar
 * autenticado", sin ningún permiso concreto — por eso un usuario {@code PENDING_ONBOARDING}
 * (a quien RN-03 SPEC-AUTH-006 le bloquearía cualquier autorización por permiso) puede igual
 * consultar su propia identidad (§7, caso límite explícito).</p>
 */
@Service
public class GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;

    public GetCurrentUserUseCase(UserRepositoryPort userRepository, RoleRepositoryPort roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public Optional<CurrentUserView> handle(UUID userId) {
        return userRepository.findById(userId).map(this::toView);
    }

    private CurrentUserView toView(User user) {
        if (user.status() == UserStatus.PENDING_ONBOARDING) {
            // §7 caso límite: rol/permiso de onboarding son conceptuales — no existe una
            // UserRoleAssignment real para ONBOARDING_USER (ver Fase 5/6: el propio estado ya
            // implica esto, sin necesitar una fila de asignación en el agregado User).
            return new CurrentUserView(user.id(), user.provider(), user.username(), user.displayName(),
                    user.email().orElse(null), user.status(),
                    Set.of(WellKnownRoles.ONBOARDING_USER), Set.of(WellKnownPermissions.VIEW_ONBOARDING_INFO));
        }

        Set<Role> roles = user.roles().stream()
                .map(UserRoleAssignment::roleId)
                .map(roleRepository::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> roleNames = roles.stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
        Set<String> permissionNames = roles.stream()
                .flatMap(role -> role.effectivePermissions().stream())
                .map(Permission::name)
                .collect(Collectors.toUnmodifiableSet());

        return new CurrentUserView(user.id(), user.provider(), user.username(), user.displayName(),
                user.email().orElse(null), user.status(), roleNames, permissionNames);
    }
}
