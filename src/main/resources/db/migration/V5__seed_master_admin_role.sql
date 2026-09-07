-- Fase 12 (Master Admin) — SPEC-AUTH-009, SPEC-AUTH-010, ADR-019, INV-AUTH-011, INV-AUTH-013.
-- Siembra el Role de sistema MASTER_ADMIN y el catálogo de permisos administrativos ya aprobado
-- en SPEC-AUTH-010 §10 (API Contract). Un Role de sistema no puede quedar sin permisos
-- (INV-AUTH-011): se crea ya con el catálogo completo, no vacío a la espera de Fase 17.
--
-- No se siembra aquí el Role ONBOARDING_USER (también de sistema, INV-AUTH-004): ningún código
-- hasta esta fase lo asigna realmente a un UserRoleAssignment (ver Fase 5/6 — un usuario sin
-- roles queda PENDING_ONBOARDING sin necesitar esa fila), así que sembrarlo ahora sería
-- especulativo; se añade en la fase que efectivamente lo consuma.

INSERT INTO permissions (name, description, is_system_permission, active) VALUES
    ('USER_MANAGE', 'Alta, actualización y cambios de estado de usuarios LOCAL (SPEC-AUTH-010)', 0, 1),
    ('ROLE_MANAGE', 'Alta, modificación y activación/desactivación de Roles (SPEC-AUTH-010)', 0, 1),
    ('ROLE_READ', 'Lectura de Roles (SPEC-AUTH-010)', 0, 1),
    ('PERMISSION_MANAGE', 'Alta, modificación y activación/desactivación de Permissions (SPEC-AUTH-010)', 0, 1),
    ('PERMISSION_READ', 'Lectura de Permissions (SPEC-AUTH-010)', 0, 1),
    ('ROLE_ASSIGN', 'Asignación explícita de un Role a un usuario (SPEC-AUTH-010)', 0, 1),
    ('ROLE_REVOKE', 'Revocación de una asignación GRANTED_EXPLICITLY (SPEC-AUTH-010)', 0, 1),
    ('AD_MAPPING_MANAGE', 'Administración del mapping AD Group -> Role (SPEC-AUTH-008)', 0, 1);

-- El id se genera aquí (no vía aplicación) porque esta fila nace de una migración, no de un
-- alta administrativa ordinaria; UUID fijo y estable para no depender de una función de la base.
INSERT INTO roles (id, name, description, is_system_role, active) VALUES
    ('00000000-0000-0000-0000-000000000001', 'MASTER_ADMIN',
     'Administrador con acceso total a la administración del sistema (SPEC-AUTH-009)', 1, 1);

INSERT INTO role_permissions (role_id, permission_name)
SELECT '00000000-0000-0000-0000-000000000001', name FROM permissions
WHERE name IN ('USER_MANAGE', 'ROLE_MANAGE', 'ROLE_READ', 'PERMISSION_MANAGE', 'PERMISSION_READ',
               'ROLE_ASSIGN', 'ROLE_REVOKE', 'AD_MAPPING_MANAGE');
