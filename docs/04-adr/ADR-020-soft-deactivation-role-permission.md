# ADR-020 — Soft deactivation (sin eliminación física) de Role y Permission

## 1. Estado
Aceptado

## 2. Fecha
2026-09-06

## 3. Contexto
La nueva capacidad de administración (`SPEC-AUTH-010`) permite crear y retirar Roles y Permissions. `SecurityAuditEvent` es append-only y `UserRoleAssignment`/`AdGroupRoleMapping` referencian `Role`/`Permission` por identificador. Es necesario decidir qué significa "retirar" un Role o una Permission sin romper la integridad histórica de esos registros.

## 4. Decisión
`Role` y `Permission` nunca se eliminan físicamente. Su ciclo de vida administrativo se expresa mediante un atributo `active` (booleano). Cuando `active = false`:
- Un `Role` inactivo no otorga ninguno de sus permisos a los usuarios que lo poseen, aunque sus `UserRoleAssignment` permanezcan intactos.
- Una `Permission` inactiva no cuenta en ninguna evaluación de autorización (`SPEC-AUTH-006`), aunque siga asociada a los `Role` que la incluían.

Los Roles/Permissions marcados como de sistema (`isSystemRole`/`isSystemPermission = true`) no pueden desactivarse (ya cubierto por `INV-AUTH-011` para Role; extendido a Permission en esta decisión).

## 5. Alternativas consideradas

### Alternativa A — Eliminación física (DELETE)
**Ventajas:** modelo más simple, sin un estado adicional que considerar.
**Desventajas:** rompe la integridad referencial de `UserRoleAssignment` y `AdGroupRoleMapping` históricos, y de cualquier `SecurityAuditEvent.metadata` que haga referencia al nombre del Role/Permission; un `SecurityAuditEvent` es append-only por diseño (`ADR-012`) y no debería quedar huérfano de contexto.

### Alternativa B — Soft deactivation mediante atributo `active` (elegida)
**Ventajas:** preserva la integridad histórica; el efecto en autorización es inmediato (consistente con `ADR-006`, DB como fuente de verdad); es reversible (reactivar) sin perder la configuración previa (permisos del rol, etc.).
**Desventajas:** el catálogo de Roles/Permissions crece indefinidamente con entradas inactivas; requiere que las consultas administrativas (`GET /auth/admin/roles`, `GET /auth/admin/permissions`) filtren o señalicen claramente el estado `active`.

## 6. Consecuencias

### Positivas
- Ninguna operación administrativa de este módulo puede corromper la trazabilidad histórica de auditoría o de asignaciones.
- El efecto de desactivar un Role/Permission es inmediato, consistente con el resto del modelo de autorización.

### Negativas
- Acumulación de entradas inactivas en el catálogo a largo plazo (aceptado; no se identifica una necesidad real de purga en V1).

### Riesgos
- Ninguno relevante adicional.

## 7. Áreas afectadas
Domain (`Role`, `Permission`), Application (`SPEC-AUTH-010`), Persistence.

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-029, REQ-AUTH-030
- Specifications: SPEC-AUTH-010
- Dominio: Role, Permission (INV-AUTH-011, INV-AUTH-014)
- ADR relacionados: ADR-006, ADR-012
