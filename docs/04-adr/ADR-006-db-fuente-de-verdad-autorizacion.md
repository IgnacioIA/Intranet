# ADR-006 — Base de datos como fuente de verdad de autorización (sin cache en V1)

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Un JWT puede embeber roles/permisos como claims, lo cual es eficiente pero introduce autorización obsoleta: revocar un permiso no tendría efecto hasta que expire el token. Es necesario decidir cuál es la fuente de verdad real de autorización en cada request.

## 4. Decisión
Cada request protegido evalúa el estado (`User.status`), roles y permisos vigentes consultando la base de datos. El JWT contiene únicamente los claims necesarios para identificar al usuario y validar la sesión (ver ADR-007), nunca la fuente de verdad de autorización. No se implementa una capa de cache (Redis u otra) en V1; se deja preparado un punto de extensión (Port de lectura de autorización) que permitiría introducir cache sin modificar el dominio ni la capa de aplicación.

## 5. Alternativas consideradas

### Alternativa A — Roles/permisos embebidos en el JWT como fuente de verdad
**Ventajas:** máxima eficiencia, sin consulta a DB por request.
**Desventajas:** autorización obsoleta hasta la expiración del token; contradice REQ-AUTH-015.

### Alternativa B — DB como fuente de verdad en cada request, sin cache (elegida)
**Ventajas:** cambios de permisos/roles/estado tienen efecto inmediato.
**Desventajas:** una consulta adicional por request protegido (aceptable a la escala actual; extensible con cache si se vuelve necesario).

## 6. Consecuencias

### Positivas
- Revocar, deshabilitar o cambiar permisos tiene efecto inmediato, sin esperar expiración de token.

### Negativas
- Costo de una consulta adicional por request protegido.

### Riesgos
- A mayor escala, esta consulta podría convertirse en cuello de botella. Mitigación prevista pero no implementada en V1: introducir un `AuthorizationSnapshotPort` con adapter Redis cuando el volumen lo justifique, sin cambiar el dominio.

## 7. Áreas afectadas
Application (verificación de autorización en el borde), Infrastructure (consulta a DB en cada request protegido).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-015
- Specifications: SPEC-AUTH-006
- Dominio: User, UserRoleAssignment (INV-AUTH-012)
- ADR relacionados: ADR-007
