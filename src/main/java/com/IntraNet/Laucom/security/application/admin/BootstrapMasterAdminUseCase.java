package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.MasterAdminBootstrapRequiredException;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-AUTH-014 SPEC-AUTH-009: garantiza que exista al menos un administrador local, invocado en
 * el arranque de la aplicación (ver {@code infrastructure.bootstrap.MasterAdminBootstrapRunner}).
 *
 * <p>El Role de sistema {@code MASTER_ADMIN} no lo crea este Use Case: se siembra por migración
 * (V5, Fase 12) junto con el catálogo de permisos administrativos de SPEC-AUTH-010, porque
 * INV-AUTH-011 exige que un Role de sistema nunca quede sin permisos, y ese catálogo ya está
 * aprobado — no depende de que la administración (Fase 17) exista todavía.</p>
 */
@Service
public class BootstrapMasterAdminUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final AuditPort auditPort;
    private final Clock clock;
    private final String username;
    private final String displayName;
    private final String email;
    private final String bootstrapPasswordHash;

    public BootstrapMasterAdminUseCase(UserRepositoryPort userRepository, RoleRepositoryPort roleRepository,
                                        AuditPort auditPort, Clock clock,
                                        @Value("${master-admin.username:admin}") String username,
                                        @Value("${master-admin.display-name:Master Admin}") String displayName,
                                        @Value("${master-admin.email:}") String email,
                                        @Value("${master-admin.bootstrap-password-hash:}") String bootstrapPasswordHash) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditPort = auditPort;
        this.clock = clock;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.bootstrapPasswordHash = bootstrapPasswordHash;
    }

    public void handle() {
        Instant now = clock.instant();
        Role masterAdminRole = roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "El Role de sistema MASTER_ADMIN no existe — falta aplicar la migración V5"));

        if (userRepository.existsActiveUserWithRole(masterAdminRole.id())) {
            return; // UC-AUTH-014, flujo alternativo: ya existe un administrador -> idempotente.
        }

        if (bootstrapPasswordHash == null || bootstrapPasswordHash.isBlank() || !isArgon2Hash(bootstrapPasswordHash)) {
            // RN-07 SPEC-AUTH-009, ADR-019: fail-fast. Debe propagarse hasta abortar el arranque.
            throw new MasterAdminBootstrapRequiredException(
                    "No existe ningún administrador local y no se configuró un secreto de bootstrap válido "
                            + "(master-admin.bootstrap-password-hash: hash Argon2id pre-generado, RN-01/RN-02)");
        }

        User admin = User.createLocal(UUID.randomUUID(), username, displayName,
                (email == null || email.isBlank()) ? null : email,
                PasswordCredential.of(bootstrapPasswordHash, true), now); // RN-03: mustChangeOnNextLogin.
        admin.assignRole(UserRoleAssignment.grantedExplicitly(masterAdminRole.id(), now), now);
        userRepository.save(admin);

        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.MASTER_ADMIN_BOOTSTRAPPED,
                now, admin.id(), admin.id(), "startup", AuditOutcome.SUCCESS, Map.of("username", username)));
    }

    /** RN: "secreto externo ausente o con formato inválido" — verificación mínima de forma, no de fuerza. */
    private static boolean isArgon2Hash(String value) {
        return value.startsWith("$argon2");
    }
}
