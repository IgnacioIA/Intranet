-- Fase 14 (Security Audit) — ADR-012 (doble escritura DB + log estructurado), entities.md
-- "SecurityAuditEvent", INV-AUTH-009.
--
-- `metadata` se guarda como TEXT con contenido JSON serializado por la aplicación, no como
-- columna JSON nativa de MySQL: evita cualquier duda de compatibilidad entre el tipo JSON nativo
-- y esta versión de Hibernate ORM (7.4.1, todavía reciente) sin poder verificarla contra un
-- MySQL real en este entorno — simplificación deliberada y de bajo riesgo para V1; no se
-- consulta metadata mediante funciones JSON de MySQL en ningún punto del módulo todavía.

CREATE TABLE security_audit_event (
    id                CHAR(36)      NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    occurred_at       DATETIME(6)   NOT NULL,
    actor_user_id     CHAR(36)      NULL,
    subject_user_id   CHAR(36)      NULL,
    correlation_id    VARCHAR(100)  NOT NULL,
    outcome           VARCHAR(20)   NOT NULL,
    metadata          TEXT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_security_audit_event_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT fk_security_audit_event_subject
        FOREIGN KEY (subject_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_security_audit_event_occurred_at ON security_audit_event (occurred_at);
CREATE INDEX idx_security_audit_event_correlation_id ON security_audit_event (correlation_id);
