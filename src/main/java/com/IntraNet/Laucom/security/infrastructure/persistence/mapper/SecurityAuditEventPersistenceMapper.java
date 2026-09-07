package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.SecurityAuditEventJpaEntity;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.JacksonException;

/**
 * {@code metadata} se serializa aquí (no en la entidad JPA, que solo conoce texto — ver
 * V7__create_security_audit_event.sql). Usa Jackson 3.x ({@code tools.jackson.databind}, no el
 * clásico {@code com.fasterxml.jackson.databind}): es la librería JSON de primera clase de
 * Spring Boot 4.1/Spring Framework 7 en este proyecto (`spring-boot-jackson` depende de
 * `tools.jackson.core:jackson-databind`); el `com.fasterxml.jackson.core:jackson-databind`
 * presente en el classpath es solo una dependencia interna, en tiempo de ejecución, de
 * {@code jjwt-jackson} (Fase 8) — no forma parte del compile classpath del proyecto.
 */
public final class SecurityAuditEventPersistenceMapper {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private SecurityAuditEventPersistenceMapper() {
    }

    public static SecurityAuditEventJpaEntity toNewJpa(SecurityAuditEvent event) {
        String metadataJson;
        try {
            metadataJson = OBJECT_MAPPER.writeValueAsString(event.metadata());
        } catch (JacksonException e) {
            // No debería ocurrir: metadata es Map<String,String>, siempre serializable. Si
            // sucediera, se prefiere perder solo el detalle de metadata antes que el evento
            // entero (ADR-012: la auditoría misma no debe fallar por esto).
            metadataJson = null;
        }
        return new SecurityAuditEventJpaEntity(event.id().toString(), event.eventType().name(), event.occurredAt(),
                event.actorUserId().map(Object::toString).orElse(null),
                event.subjectUserId().map(Object::toString).orElse(null),
                event.correlationId(), event.outcome().name(), metadataJson);
    }
}
