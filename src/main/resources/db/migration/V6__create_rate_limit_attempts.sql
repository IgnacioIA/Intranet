-- Fase 13 (Rate Limiting) — ADR-013, REQ-AUTH-018, REQ-AUTH-022.
-- Ventana deslizante basada en un log de intentos (append-only): cada llamada a
-- RateLimiterPort.checkAndRecord inserta una fila y cuenta las filas de esa misma clave dentro
-- de la ventana vigente. Válido en despliegue multi-instancia (estado compartido en DB, no en
-- memoria de proceso) sin introducir Redis todavía (ADR-013 lo deja como adapter futuro).
--
-- Crecimiento no acotado explícitamente en V1: el propio ADR-013 anticipa que el volumen podría
-- requerir optimización (índices adicionales, particionado, purga) antes de justificar Redis —
-- no se resuelve especulativamente aquí.

CREATE TABLE rate_limit_attempts (
    id             CHAR(36)      NOT NULL,
    rate_key       VARCHAR(200)  NOT NULL,
    attempted_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Respalda tanto el conteo (rate_key + rango de attempted_at) como una eventual purga futura.
CREATE INDEX idx_rate_limit_attempts_key_time ON rate_limit_attempts (rate_key, attempted_at);
