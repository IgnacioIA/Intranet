-- SPEC-AUTH-004 (revocación administrativa de sesiones): permiso faltante, igual patrón que
-- V8__add_user_read_permission.sql. Se sigue sembrando por migración (no únicamente por el nuevo
-- Permission Catalog, ADR-021) porque el Master Admin necesita poder ejecutar esta operación
-- desde el primer arranque posterior al despliegue, y el join con role_permissions debe existir
-- antes de que cualquier request lo requiera — el Permission Catalog (ApplicationRunner) solo
-- crea el Permission si falta, nunca decide a qué Role otorgarlo.

-- is_system_permission = 0: mismo criterio que el resto de los permisos administrativos ya
-- sembrados en V5/V8 (USER_MANAGE, AD_MAPPING_MANAGE, etc.) — administrable como cualquier otro
-- Permission de negocio, no protegido incondicionalmente contra desactivación (RN-09 solo aplica
-- a permisos realmente de sistema, de los cuales ninguno de esta familia lo es en la práctica).
INSERT INTO permissions (name, description, is_system_permission, active) VALUES
    ('SESSION_REVOKE_ANY', 'Revocación administrativa de las sesiones de un usuario (SPEC-AUTH-004)', 0, 1);

INSERT INTO role_permissions (role_id, permission_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'SESSION_REVOKE_ANY');
