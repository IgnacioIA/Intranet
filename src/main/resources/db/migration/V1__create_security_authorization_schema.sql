-- Módulo de Seguridad (AUTH) — Fase 2 (Persistencia).
-- Esquema derivado de docs/02-domain/entities.md. No se agregan tablas ni columnas
-- que no estén respaldadas por una SPEC/ADR/INV ya aprobados.
--
-- Alcance de esta migración: User, Role, Permission, UserRoleAssignment.
-- RefreshToken, PasswordRecoveryToken, AdGroupRoleMapping y SecurityAuditEvent se agregan
-- en sus propias migraciones cuando se implementen las fases que los necesitan (9, 7, 5/8, 14),
-- para mantener el esquema evolucionando junto con la funcionalidad real (proporcionalidad,
-- .claude/core/principles.md).

CREATE TABLE users (
    id                                   CHAR(36)      NOT NULL,
    provider                             VARCHAR(20)   NOT NULL,
    external_id                          VARCHAR(64)   NULL,
    username                             VARCHAR(100)  NOT NULL,
    display_name                         VARCHAR(200)  NULL,
    email                                VARCHAR(255)  NULL,
    status                               VARCHAR(30)   NOT NULL,
    password_hash                        VARCHAR(255)  NULL,
    must_change_password_on_next_login   TINYINT(1)    NOT NULL DEFAULT 0,
    created_at                           DATETIME(6)   NOT NULL,
    last_login_at                        DATETIME(6)   NULL,
    last_ad_sync_at                      DATETIME(6)   NULL,
    failed_login_attempts                INT           NOT NULL DEFAULT 0,
    locked_until                         DATETIME(6)   NULL,
    PRIMARY KEY (id),
    -- INV-AUTH-001: unicidad de (provider, username).
    CONSTRAINT uk_users_provider_username UNIQUE (provider, username),
    -- INV-AUTH-002: unicidad de external_id (objectGUID). NULL permitido y no-único en MySQL
    -- (varios usuarios LOCAL con external_id NULL no violan esta constraint).
    CONSTRAINT uk_users_external_id UNIQUE (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    -- Permission es un Value Object identificado por `name` (entities.md): sin id sustituto.
    name                    VARCHAR(100)  NOT NULL,
    description             VARCHAR(500)  NULL,
    is_system_permission    TINYINT(1)    NOT NULL DEFAULT 0,
    active                  TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
    id              CHAR(36)      NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    description     VARCHAR(500)  NULL,
    is_system_role  TINYINT(1)    NOT NULL DEFAULT 0,
    active          TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
    role_id           CHAR(36)      NOT NULL,
    permission_name   VARCHAR(100)  NOT NULL,
    PRIMARY KEY (role_id, permission_name),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_name) REFERENCES permissions (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_role_assignments (
    -- INV-AUTH-005 / INV-AUTH-015: (user_id, role_id) es la clave — un mismo Role nunca se
    -- asigna dos veces al mismo usuario, independientemente de la procedencia.
    user_id            CHAR(36)      NOT NULL,
    role_id            CHAR(36)      NOT NULL,
    provenance         VARCHAR(30)   NOT NULL,
    source_ad_group    VARCHAR(200)  NULL,
    assigned_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_assignments_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_role_assignments_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
