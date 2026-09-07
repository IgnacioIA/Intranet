package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * RN-16 SPEC-AUTH-010: toda operación administrativa de esta SPEC audita tanto
 * {@code AUTHORIZATION_DENIED} (ya lo hace {@link AuthorizationService#requireAuthorized}) como
 * {@code AUTHORIZATION_GRANTED} — a diferencia del resto del módulo, donde
 * {@code AUTHORIZATION_GRANTED} solo se audita para operaciones administrativas sensibles
 * específicas (política del Decision Ledger, `docs/03-architecture/security.md §5`). Esta clase
 * concentra ese patrón para no repetirlo en cada Use Case de administración.
 */
@Service
public class AdminActionAuthorizer {

    private final AuthorizationService authorizationService;
    private final AuditPort auditPort;
    private final Clock clock;

    public AdminActionAuthorizer(AuthorizationService authorizationService, AuditPort auditPort, Clock clock) {
        this.authorizationService = authorizationService;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    /** Lanza {@code InsufficientPermissionException} (y audita DENIED) si no está autorizado; si lo está, audita GRANTED. */
    public void require(UUID actorId, String permission, String correlationId) {
        authorizationService.requireAuthorized(actorId, permission, correlationId);
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.AUTHORIZATION_GRANTED,
                clock.instant(), actorId, actorId, correlationId, AuditOutcome.SUCCESS,
                Map.of("permission", permission)));
    }
}
