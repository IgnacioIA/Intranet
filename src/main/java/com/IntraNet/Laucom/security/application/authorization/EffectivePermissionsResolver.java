package com.IntraNet.Laucom.security.application.authorization;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resuelve el conjunto de nombres de {@link Permission} vigentes de un {@link User} a partir de
 * sus roles (unión, ADR-020 respeta roles inactivos como sin efecto). Usado para construir el
 * claim informativo {@code permissions} del Access Token (Fase 8, nunca autoritativo — ADR-006).
 *
 * <p>Misma lógica de recorrido de roles que la evaluación interna de
 * {@link AuthorizationService}, extraída aquí como utilidad reutilizable en vez de duplicada por
 * segunda vez en la emisión de tokens (Fase 9). No se modificó {@code AuthorizationService} para
 * reutilizar esto — es un servicio ya verificado en Fases 6/7 y este cambio no altera ningún
 * comportamiento aprobado, así que se prefirió no tocarlo.</p>
 */
@Service
public class EffectivePermissionsResolver {

    private final RoleRepositoryPort roleRepository;

    public EffectivePermissionsResolver(RoleRepositoryPort roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Set<String> resolve(User user) {
        return user.roles().stream()
                .map(UserRoleAssignment::roleId)
                .map(roleRepository::findById)
                .flatMap(Optional::stream)
                .flatMap(role -> role.effectivePermissions().stream())
                .map(Permission::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
