-- Fase 5 (Active Directory) — SPEC-AUTH-001 (UC-AUTH-003/004), SPEC-AUTH-008, INV-AUTH-010.
-- Ver el comentario de alcance en V1__create_security_authorization_schema.sql: esta tabla se
-- agrega recién ahora, junto con la funcionalidad que la necesita.

CREATE TABLE ad_group_role_mappings (
    id                    CHAR(36)      NOT NULL,
    ad_group_identifier   VARCHAR(200)  NOT NULL,
    role_id               CHAR(36)      NOT NULL,
    -- AdGroupRoleMapping.create() inicializa updated_by = created_by y updated_at = created_at
    -- (domain/model/AdGroupRoleMapping.java): ninguno de los cuatro queda nulo tras el alta.
    created_by            CHAR(36)      NOT NULL,
    updated_by            CHAR(36)      NOT NULL,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    -- INV-AUTH-010: un grupo AD mapea a lo sumo a un rol.
    CONSTRAINT uk_ad_group_role_mappings_group UNIQUE (ad_group_identifier),
    CONSTRAINT fk_ad_group_role_mappings_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_ad_group_role_mappings_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_ad_group_role_mappings_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
