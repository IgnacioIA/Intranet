package com.IntraNet.Laucom.security.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entidad append-only. Ver entities.md "SecurityAuditEvent", INV-AUTH-009, ADR-012.
 *
 * <p>La garantía de no incluir datos sensibles en {@code metadata} (contraseñas, tokens,
 * secretos — REQ-AUTH-023) es responsabilidad del llamador (Application/Infrastructure); este
 * tipo no interpreta ni filtra el contenido de {@code metadata}.</p>
 */
public final class SecurityAuditEvent {

    private final UUID id;
    private final SecurityEventType eventType;
    private final Instant occurredAt;
    private final UUID actorUserId;
    private final UUID subjectUserId;
    private final String correlationId;
    private final AuditOutcome outcome;
    private final Map<String, String> metadata;

    private SecurityAuditEvent(UUID id, SecurityEventType eventType, Instant occurredAt, UUID actorUserId,
                                UUID subjectUserId, String correlationId, AuditOutcome outcome,
                                Map<String, String> metadata) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.actorUserId = actorUserId;
        this.subjectUserId = subjectUserId;
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SecurityAuditEvent occur(UUID id, SecurityEventType eventType, Instant occurredAt,
                                            UUID actorUserId, UUID subjectUserId, String correlationId,
                                            AuditOutcome outcome, Map<String, String> metadata) {
        return new SecurityAuditEvent(id, eventType, occurredAt, actorUserId, subjectUserId, correlationId, outcome, metadata);
    }

    public UUID id() {
        return id;
    }

    public SecurityEventType eventType() {
        return eventType;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Optional<UUID> actorUserId() {
        return Optional.ofNullable(actorUserId);
    }

    public Optional<UUID> subjectUserId() {
        return Optional.ofNullable(subjectUserId);
    }

    public String correlationId() {
        return correlationId;
    }

    public AuditOutcome outcome() {
        return outcome;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
