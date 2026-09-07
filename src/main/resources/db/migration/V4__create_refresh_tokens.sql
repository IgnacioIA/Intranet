-- Fase 9 (Refresh Tokens) — SPEC-AUTH-002, ADR-007, ADR-008, INV-AUTH-006, INV-AUTH-007.
-- RefreshTokenFamily no tiene tabla propia (entities.md: "concepto, no necesariamente tabla
-- propia"): family_id es una columna más, compartida por los tokens de una misma cadena de
-- rotación.

CREATE TABLE refresh_tokens (
    id                     CHAR(36)      NOT NULL,
    user_id                CHAR(36)      NOT NULL,
    token_hash             VARCHAR(255)  NOT NULL,
    family_id              CHAR(36)      NOT NULL,
    issued_at              DATETIME(6)   NOT NULL,
    expires_at             DATETIME(6)   NOT NULL,
    revoked_at             DATETIME(6)   NULL,
    replaced_by_token_id   CHAR(36)      NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ambos índices respaldan las operaciones de revocación masiva de RefreshTokenRepositoryPort
-- (revokeAllActiveInFamily, revokeAllActiveForUser) y la búsqueda de tokens vigentes por usuario.
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
