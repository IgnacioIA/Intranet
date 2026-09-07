# ADR-012 — Auditoría de seguridad con doble escritura (base de datos + log estructurado) vía AuditPort

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Es necesario decidir dónde y cómo se almacena la auditoría de seguridad (distinta de los logs técnicos), y si se justifica una abstracción (`AuditPort`) dado que introducir múltiples adapters especulativos sería sobreingeniería.

## 4. Decisión
Se define `AuditPort` en la capa de aplicación, con una única implementación en V1 que escribe simultáneamente: (a) una fila en una tabla `security_audit_event`, y (b) una línea de log estructurado (JSON) con el mismo contenido. El dominio no conoce `AuditPort` directamente.

**Corrección (2026-09-06, Fase 14 de implementación):** el mecanismo de disparo original de esta decisión — "los Use Cases publican eventos de aplicación/dominio que un listener traduce a llamadas al puerto" — nunca se implementó así. Desde la Fase 3 (inclusive), todos los Use Cases que necesitan auditar (`ChangePasswordUseCase`, `ConfirmPasswordRecoveryUseCase`, `RequestPasswordRecoveryUseCase`, `AuthenticateLocalUserUseCase`, `AuthenticateActiveDirectoryUserUseCase`, `AuthorizationService`, `RenewSessionUseCase`, `LogoutUseCase`, `LogoutAllUseCase`, `BootstrapMasterAdminUseCase`) invocan `AuditPort.record(...)` **directamente**, sin ningún evento ni listener intermedio. Al llegar a la Fase 14 (adapter concreto de `AuditPort`) se detectó el desajuste entre esta ADR y la implementación real ya aprobada y testeada en 9 Use Cases; se decidió (decisión humana explícita) mantener la llamada directa y corregir esta ADR, en vez de retrofitear un refactor grande y riesgoso sin un beneficio concreto identificado — coherente con el criterio general de ADR-015 ("cada concern usa la herramienta más simple que lo resuelve correctamente"): no existe hoy ningún caso que requiera múltiples consumidores independientes del mismo evento de auditoría, que es la justificación típica de introducir esa indirección.

## 5. Alternativas consideradas

### Alternativa A — Solo base de datos
**Ventajas:** consultable directamente, útil para pantallas de auditoría futuras.
**Desventajas:** si la escritura a DB falla momentáneamente, se pierde el evento sin otro canal de respaldo.

### Alternativa B — Solo log estructurado
**Ventajas:** barato, no depende de la disponibilidad de la base de datos.
**Desventajas:** no es fácilmente consultable/filtrable para una pantalla de auditoría sin herramientas adicionales.

### Alternativa C — Doble escritura vía un único AuditPort (elegida)
**Ventajas:** combina consultabilidad (DB) con un canal de respaldo barato y resistente (log); un solo adapter, sin sobreingeniería de múltiples destinos plugables.
**Desventajas:** cierta redundancia de almacenamiento, aceptable dado el bajo volumen relativo de eventos de seguridad frente al tráfico general.

## 6. Consecuencias

### Positivas
- Resiliencia ante fallo puntual de un canal; base para una futura pantalla de auditoría sin rediseño.

### Negativas
- Redundancia de almacenamiento (aceptada).

### Riesgos
- No se implementa adapter hacia SIEM en V1 (postergado explícitamente); si se necesitara, se agregaría como una segunda implementación del mismo Port sin tocar Application/Domain.

## 7. Áreas afectadas
Application (`AuditPort`, invocado directamente por cada Use Case — ver corrección en §4), Infrastructure (adapter DB + log), Persistence (`SecurityAuditEvent`).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-017, REQ-AUTH-023, REQ-AUTH-024
- Specifications: todas las de este módulo
- Dominio: SecurityAuditEvent (INV-AUTH-009)
