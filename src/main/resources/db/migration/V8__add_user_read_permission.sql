-- Fase 17 (Administración, SPEC-AUTH-010 UC-AUTH-015) — permiso faltante en la siembra de la
-- Fase 12 (V5): USER_READ es distinto de USER_MANAGE (lectura vs. escritura, §10 de la SPEC).

INSERT INTO permissions (name, description, is_system_permission, active) VALUES
    ('USER_READ', 'Consulta de usuarios, lista y detalle (SPEC-AUTH-010)', 0, 1);

INSERT INTO role_permissions (role_id, permission_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'USER_READ');
