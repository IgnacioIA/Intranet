package com.IntraNet.Laucom.security.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Mapeo JPA de {@link com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent}. Ver
 * ADR-012, INV-AUTH-009. {@code metadata} se persiste como texto JSON ya serializado por el
 * mapper (ver comentario en la migración V7): esta entidad no interpreta su contenido.
 */
@Entity
@Table(name = "security_audit_event")
public class SecurityAuditEventJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", length = 36)
    private String actorUserId;

    @Column(name = "subject_user_id", length = 36)
    private String subjectUserId;

    @Column(name = "correlation_id", length = 100, nullable = false)
    private String correlationId;

    @Column(name = "outcome", length = 20, nullable = false)
    private String outcome;

    @Lob
    @Column(name = "metadata")
    private String metadata;

    protected SecurityAuditEventJpaEntity() {
        // JPA
    }

    public SecurityAuditEventJpaEntity(String id, String eventType, Instant occurredAt, String actorUserId,
                                        String subjectUserId, String correlationId, String outcome, String metadata) {
        this.id = id;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.subjectUserId = subjectUserId;
        this.correlationId = correlationId;
        this.outcome = outcome;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public String getSubjectUserId() {
        return subjectUserId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getMetadata() {
        return metadata;
    }
}
