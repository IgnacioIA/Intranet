package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.SecurityAuditEventPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.SecurityAuditEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ADR-012: única implementación de {@link AuditPort} en V1 — doble escritura (DB + log
 * estructurado) como dos canales independientes y redundantes, no como una operación atómica:
 * el fallo de uno no debe impedir el otro (esa es exactamente la ventaja que justifica la
 * decisión frente a un solo canal). Invocado directamente por cada Use Case — ver la corrección
 * de mecanismo documentada en ADR-012 §4 (Fase 14): no hay eventos de aplicación ni listener
 * intermedio.
 */
@Component
public class DualWriteAuditAdapter implements AuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(DualWriteAuditAdapter.class);
    /** Logger dedicado y nombrado explícitamente: permite enrutarlo a un destino propio (archivo,
     * agregador) mediante configuración de logging estándar, sin acoplar este adapter a esa decisión. */
    private static final Logger SECURITY_AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final SecurityAuditEventJpaRepository repository;

    public DualWriteAuditAdapter(SecurityAuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(SecurityAuditEvent event) {
        writeToDatabase(event);
        writeToStructuredLog(event);
    }

    private void writeToDatabase(SecurityAuditEvent event) {
        try {
            repository.save(SecurityAuditEventPersistenceMapper.toNewJpa(event));
        } catch (RuntimeException e) {
            LOG.error("No se pudo escribir SecurityAuditEvent en base de datos (id={}, type={})",
                    event.id(), event.eventType(), e);
        }
    }

    private void writeToStructuredLog(SecurityAuditEvent event) {
        try {
            SECURITY_AUDIT_LOG.info(OBJECT_MAPPER.writeValueAsString(toLogPayload(event)));
        } catch (RuntimeException e) {
            LOG.error("No se pudo escribir SecurityAuditEvent en el log estructurado (id={})", event.id(), e);
        }
    }

    /** Mapa ordenado y explícito en vez de serializar el record del dominio directamente: evita
     * acoplar el formato del log a la forma interna de {@link SecurityAuditEvent}. */
    private Map<String, Object> toLogPayload(SecurityAuditEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", event.id().toString());
        payload.put("eventType", event.eventType().name());
        payload.put("occurredAt", event.occurredAt().toString());
        payload.put("actorUserId", event.actorUserId().map(Object::toString).orElse(null));
        payload.put("subjectUserId", event.subjectUserId().map(Object::toString).orElse(null));
        payload.put("correlationId", event.correlationId());
        payload.put("outcome", event.outcome().name());
        payload.put("metadata", event.metadata()); // INV-AUTH-009: nunca contiene datos sensibles (responsabilidad del llamador).
        return payload;
    }
}
