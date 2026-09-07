package com.IntraNet.Laucom.security.application.authentication;

import com.IntraNet.Laucom.security.application.exception.DirectoryGroupLookupFailedException;
import com.IntraNet.Laucom.security.application.exception.DirectoryUnavailableException;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.session.SessionIssuer;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC-AUTH-002 SPEC-AUTH-001: autenticación de usuarios {@code ACTIVE_DIRECTORY}, con
 * aprovisionamiento de Shadow Identity (UC-AUTH-003) y sincronización de roles derivados
 * (UC-AUTH-004) embebidos, tal como los define la propia SPEC ("Actor: Sistema, disparado
 * dentro de UC-AUTH-002").
 *
 * <p>Igual que {@link AuthenticateLocalUserUseCase}: desde la Fase 9 emite la sesión completa
 * (Access Token + Refresh Token, vía {@link SessionIssuer}) en cada login exitoso. El endpoint
 * HTTP en sí (con el transporte de cookie `HttpOnly`) sigue diferido a la Fase 10.</p>
 *
 * <p><b>Limitación conocida, señalada explícitamente (no resuelta en silencio):</b> el flujo
 * alternativo de UC-AUTH-004 "usuario detectado DISABLED/inexistente en AD durante esta
 * consulta" no es alcanzable a través de este Use Case: {@link IdentityDirectoryPort#authenticate}
 * solo devuelve {@code Authenticated} cuando el bind fue exitoso, y una cuenta deshabilitada en
 * AD no puede completar un bind exitoso — por lo tanto ese flujo, tal como está descrito, nunca
 * se dispara desde un intento de login. Detectarlo requeriría una señal adicional en
 * {@code IdentityDirectoryPort} (ej. inspeccionar {@code userAccountControl} en la propia
 * búsqueda de grupos) que el Port aprobado en Fase 1 no expone. Se reporta como GAP en el
 * checkpoint de esta fase en vez de extender el Port sin autorización.</p>
 */
@Service
public class AuthenticateActiveDirectoryUserUseCase {

    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final int MAX_ATTEMPTS_PER_IDENTITY = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    private final IdentityDirectoryPort identityDirectory;
    private final UserRepositoryPort userRepository;
    private final AdGroupRoleMappingRepositoryPort mappingRepository;
    private final RateLimiterPort rateLimiter;
    private final AuditPort auditPort;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public AuthenticateActiveDirectoryUserUseCase(IdentityDirectoryPort identityDirectory,
                                                   UserRepositoryPort userRepository,
                                                   AdGroupRoleMappingRepositoryPort mappingRepository,
                                                   RateLimiterPort rateLimiter, AuditPort auditPort,
                                                   SessionIssuer sessionIssuer, Clock clock) {
        this.identityDirectory = identityDirectory;
        this.userRepository = userRepository;
        this.mappingRepository = mappingRepository;
        this.rateLimiter = rateLimiter;
        this.auditPort = auditPort;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    public AuthenticationResult handle(AuthenticateActiveDirectoryUserCommand command, String correlationId) {
        enforceRateLimit(command, correlationId);
        Instant now = clock.instant();

        IdentityDirectoryPort.DirectoryAuthenticationResult result =
                identityDirectory.authenticate(command.username(), command.password());

        if (result instanceof IdentityDirectoryPort.CredentialsRejected) {
            audit(SecurityEventType.LOGIN_FAILURE, null, correlationId, now, AuditOutcome.FAILURE,
                    Map.of("reason", "ad-credentials-rejected"));
            throw new InvalidCredentialException(); // RN-06: idéntico al resto de los rechazos de login.
        }
        if (result instanceof IdentityDirectoryPort.DirectoryUnavailable) {
            audit(SecurityEventType.AD_CONNECTION_FAILURE, null, correlationId, now, AuditOutcome.FAILURE, Map.of());
            throw new DirectoryUnavailableException(); // 2b: excepción intencional a RN-06.
        }
        if (result instanceof IdentityDirectoryPort.GroupLookupFailed glf) {
            audit(SecurityEventType.AD_LOOKUP_FAILURE, null, correlationId, now, AuditOutcome.FAILURE,
                    Map.of("externalId", glf.externalId()));
            throw new DirectoryGroupLookupFailedException(); // RN-09, ADR-017: fail closed.
        }

        IdentityDirectoryPort.Authenticated authenticated = (IdentityDirectoryPort.Authenticated) result;
        User user = resolveUser(command.username(), authenticated, now, correlationId);

        if (!user.canAuthenticate()) {
            // 7a: status DISABLED/LOCKED tras aprovisionar o sincronizar. Mensaje genérico (RN-06).
            audit(SecurityEventType.LOGIN_FAILURE, user.id(), correlationId, now, AuditOutcome.DENIED,
                    Map.of("reason", "account-not-authenticatable", "status", user.status().name()));
            throw new InvalidCredentialException();
        }

        userRepository.save(user);
        audit(SecurityEventType.LOGIN_SUCCESS, user.id(), correlationId, now, AuditOutcome.SUCCESS, Map.of());
        return new AuthenticationResult(user, sessionIssuer.issueNewSession(user));
    }

    private User resolveUser(String username, IdentityDirectoryPort.Authenticated authenticated, Instant now,
                              String correlationId) {
        Optional<User> existing = userRepository.findByExternalId(authenticated.externalId());
        return existing.isPresent()
                ? syncExistingUser(existing.get(), username, authenticated, now, correlationId)
                : provisionNewUser(username, authenticated, now, correlationId);
    }

    /** UC-AUTH-003: alta de Shadow Identity. */
    private User provisionNewUser(String username, IdentityDirectoryPort.Authenticated authenticated, Instant now,
                                   String correlationId) {
        User user = User.provisionFromDirectory(UUID.randomUUID(), authenticated.externalId(), username,
                authenticated.displayName(), authenticated.email(), now);

        MappingEvaluation evaluation = evaluateGroups(authenticated.groupIdentifiers(), now);
        evaluation.matchedAssignments().forEach(assignment -> user.assignRole(assignment, now));
        auditUnmappedGroups(evaluation.unmappedGroups(), null, correlationId, now);

        audit(SecurityEventType.AD_USER_PROVISIONED, user.id(), correlationId, now, AuditOutcome.SUCCESS,
                Map.of("rolesAssigned", String.valueOf(evaluation.matchedAssignments().size()),
                        "groupsEvaluated", String.valueOf(authenticated.groupIdentifiers().size())));
        return user;
    }

    /** UC-AUTH-004: sincronización de roles derivados en cada login de un usuario ya existente. */
    private User syncExistingUser(User user, String username, IdentityDirectoryPort.Authenticated authenticated,
                                   Instant now, String correlationId) {
        // Atributos mutables (RN-02 SPEC-AUTH-010 no aplica a AD; ver UC-AUTH-002 paso 6). El
        // username sincronizado es el recibido en el login: el Port no devuelve un valor
        // canónico propio (ver Authenticated, Fase 1).
        user.syncDirectoryAttributes(username, authenticated.displayName(), authenticated.email());

        MappingEvaluation evaluation = evaluateGroups(authenticated.groupIdentifiers(), now);
        user.reconcileDerivedRoles(evaluation.matchedAssignments(), now);
        auditUnmappedGroups(evaluation.unmappedGroups(), user.id(), correlationId, now);
        return user;
    }

    private MappingEvaluation evaluateGroups(Set<String> groupIdentifiers, Instant now) {
        if (groupIdentifiers.isEmpty()) {
            return new MappingEvaluation(Set.of(), Set.of());
        }
        Map<String, AdGroupRoleMapping> byGroup = mappingRepository.findByAdGroupIdentifierIn(groupIdentifiers)
                .stream().collect(Collectors.toMap(AdGroupRoleMapping::adGroupIdentifier, m -> m));

        Set<UserRoleAssignment> matched = new LinkedHashSet<>();
        Set<String> unmapped = new LinkedHashSet<>();
        for (String group : groupIdentifiers) {
            AdGroupRoleMapping mapping = byGroup.get(group);
            if (mapping == null) {
                unmapped.add(group); // RN-03: sin mapping, sin rol.
            } else {
                matched.add(UserRoleAssignment.derivedFromAd(mapping.roleId(), group, now));
            }
        }
        return new MappingEvaluation(matched, unmapped);
    }

    private void auditUnmappedGroups(Set<String> unmappedGroups, UUID subjectUserId, String correlationId, Instant now) {
        unmappedGroups.forEach(group -> audit(SecurityEventType.AD_GROUP_UNMAPPED, subjectUserId, correlationId, now,
                AuditOutcome.SUCCESS, Map.of("adGroup", group)));
    }

    private void enforceRateLimit(AuthenticateActiveDirectoryUserCommand command, String correlationId) {
        RateLimiterPort.RateLimitDecision byIp =
                rateLimiter.checkAndRecord("login:ip:" + command.clientIp(), MAX_ATTEMPTS_PER_IP, RATE_LIMIT_WINDOW);
        RateLimiterPort.RateLimitDecision byIdentity = rateLimiter.checkAndRecord(
                "login:identity:AD:" + command.username(), MAX_ATTEMPTS_PER_IDENTITY, RATE_LIMIT_WINDOW);

        if (!byIp.allowed() || !byIdentity.allowed()) {
            audit(SecurityEventType.RATE_LIMIT_EXCEEDED, null, correlationId, clock.instant(), AuditOutcome.DENIED, Map.of());
            throw new RateLimitExceededException();
        }
    }

    private void audit(SecurityEventType type, UUID subjectUserId, String correlationId, Instant now,
                        AuditOutcome outcome, Map<String, String> metadata) {
        // actorUserId = subjectUserId: en login, el actor y el sujeto del evento son la misma persona.
        auditPort.record(SecurityAuditEvent.occur(UUID.randomUUID(), type, now, subjectUserId, subjectUserId,
                correlationId, outcome, metadata));
    }

    private record MappingEvaluation(Set<UserRoleAssignment> matchedAssignments, Set<String> unmappedGroups) {
    }
}
