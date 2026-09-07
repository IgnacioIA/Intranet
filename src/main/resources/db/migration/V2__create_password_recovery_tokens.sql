-- Fase 3 (Passwords) — SPEC-AUTH-007, INV-AUTH-008.

CREATE TABLE password_recovery_tokens (
    id           CHAR(36)      NOT NULL,
    user_id      CHAR(36)      NOT NULL,
    token_hash   VARCHAR(255)  NOT NULL,
    issued_at    DATETIME(6)   NOT NULL,
    expires_at   DATETIME(6)   NOT NULL,
    used_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_recovery_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_recovery_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
