package com.IntraNet.Laucom.security.application.authorization;

import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC-AUTH-006: mecanismo transversal de autorización por permiso. No es un Use Case (la SPEC
 * explícitamente no define uno, §3) sino "un servicio de aplicación explícito" (ADR-015) — la
 * alternativa elegida frente a Decorator/AOP genérico.
 *
 * <p>No cachea nada (ADR-006): cada llamada consulta {@link UserRepositoryPort} y
 * {@link RoleRepositoryPort} en el momento (RN-04, INV-AUTH-012). La integración real con
 * Spring Security en el borde (derivar {@code GrantedAuthority}, {@code @PreAuthorize}, etc. —
 * ADR-014) se conecta recién en la Fase 8/10, cuando exista un {@code SecurityContext} poblado
 * a partir de un Access Token verificado; hasta entonces, este servicio es invocable
 * directamente por cualquier Use Case de aplicación que necesite verificar autorización.</p>
 */
@Service
public class AuthorizationService {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public AuthorizationService(UserRepositoryPort userRepository, RoleRepositoryPort roleRepository,
                                 AuditPort auditPort, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    /**
     * RN-01 a RN-04 SPEC-AUTH-006. No audita por sí mismo (uso apto para, por ejemplo, decidir
     * si mostrar una opción en una UI); ver {@link #requireAuthorized} para la variante que
     * audita la denegación y lanza, pensada para proteger una operación real.
     */
    public boolean isAuthorized(UUID userId, String requiredPermission) {
        Optional<User> maybeUser = userRepository.findById(userId);
        // RN-03: ningún otro estado que ACTIVE autoriza, sin importar los permisos de sus roles.
        return maybeUser.isPresent()
                && maybeUser.get().status() == UserStatus.ACTIVE
                && hasPermission(maybeUser.get(), requiredPermission);
    }

    /**
     * Protege una operación: audita {@code AUTHORIZATION_DENIED} y lanza
     * {@link InsufficientPermissionException} si el actor no está autorizado
     * (docs/03-architecture/security.md §5: "AUTHORIZATION_DENIED se audita siempre, sin
     * excepción, en cualquier endpoint protegido"). No audita en caso de concesión: esta SPEC
     * reserva {@code AUTHORIZATION_GRANTED} solo para operaciones administrativas sensibles
     * específicas (§5, política del Decision Ledger), decisión que toma el propio Use Case
     * llamante, no este mecanismo genérico.
     */
    public void requireAuthorized(UUID userId, String requiredPermission, String correlationId) {
        if (!isAuthorized(userId, requiredPermission)) {
            auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.AUTHORIZATION_DENIED,
                    clock.instant(), userId, userId, correlationId, AuditOutcome.DENIED,
                    Map.of("requiredPermission", requiredPermission)));
            throw new InsufficientPermissionException();
        }
    }

    private boolean hasPermission(User user, String requiredPermission) {
        return user.roles().stream()
                .map(UserRoleAssignment::roleId)
                .map(roleRepository::findById)
                .flatMap(Optional::stream)
                // ADR-020: un Role inactivo no otorga ningún permiso efectivo, aunque la
                // asignación (UserRoleAssignment) siga existiendo.
                .flatMap(role -> role.effectivePermissions().stream())
                .anyMatch(permission -> permission.name().equals(requiredPermission));
    }
}
