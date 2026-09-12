# ADR-021 — Permission Catalog: declaración de permisos por la aplicación consumidora

## 1. Estado
Aprobado

## 2. Fecha
2026-09-07

## 3. Contexto
El módulo Security/Auth está diseñado para ser reutilizado por distintas aplicaciones consumidoras (ver `03-architecture/architecture.md`). Hasta ahora, todo `Permission` se creaba exclusivamente mediante: (a) migraciones Flyway del propio módulo (permisos de sistema, ej. `USER_MANAGE`), o (b) el endpoint administrativo `POST /auth/admin/permissions` (`SPEC-AUTH-010`), pensado para que un administrador dé de alta permisos de negocio de forma manual, uno por uno, desde un panel.

Ninguno de los dos mecanismos es apropiado para que una aplicación consumidora (ej. una futura Intranet) declare, versionada junto a su propio código, el conjunto de permisos que sus propias funcionalidades requieren (ej. `INTRANET.DOCUMENT_READ`). Forzar a un administrador a recrearlos manualmente cada vez que la aplicación consumidora agrega una capacidad nueva es frágil y no versionable junto al código que efectivamente los usa.

## 4. Decisión
Se introduce un mecanismo de sincronización declarativa, **Permission Catalog**, ejecutado una vez al arranque de la aplicación (`ApplicationRunner`, como ya existe para `BootstrapMasterAdminUseCase`):

- Cualquier aplicación (el propio módulo Security/Auth incluido) declara un bean `PermissionCatalog` — una lista de `PermissionDescriptor(code, description, systemPermission)`.
- Al arrancar, `SynchronizePermissionCatalogUseCase` recorre todos los `PermissionCatalog` registrados en el contexto de Spring y, para cada descriptor:
  - Si no existe un `Permission` con ese `code` en DB → lo crea, `active = true`, con el tipo (`systemPermission`) declarado.
  - Si ya existe → **no se toca en absoluto**: ni su `description`, ni su `active`, ni su `systemPermission`. La sincronización es de una sola dirección (creación de lo ausente), nunca de reconciliación bidireccional.

**Propiedad del namespace** (no impuesta técnicamente, es una convención documental): el módulo Security/Auth es dueño de sus propios permisos (históricamente sin prefijo — `USER_READ`, `ROLE_MANAGE`, etc., ver Decisión de nomenclatura más abajo); cada aplicación consumidora es dueña de su propio namespace de permisos (ej. `INTRANET.*`). El módulo Security/Auth no conoce ni necesita conocer el significado de ningún permiso de una aplicación consumidora — solo los persiste, evalúa su pertenencia a un Role, y los expone en el catálogo administrativo (`GET/POST/PATCH /auth/admin/permissions`, `SPEC-AUTH-010`) igual que a cualquier otro `Permission`.

### Decisión de nomenclatura
El diseño conceptual de esta ADR usa `AUTH.*` como ejemplo del namespace del propio módulo. **No se renombran** los permisos ya existentes (`USER_READ`, `ROLE_MANAGE`, etc.) a un prefijo `AUTH.` — serían un cambio disruptivo sobre datos y tests ya aprobados, sin valor funcional adicional (el mecanismo de reutilización no depende de que el módulo use un prefijo literal, solo de que cada aplicación consumidora use el suyo). El propio módulo declara sus permisos existentes vía este mismo mecanismo (como demostración end-to-end), conservando sus nombres actuales.

## 5. Reglas del mecanismo (Decision Ledger 2026-09-07)
1. Permiso declarado ausente en DB → se crea, `active = true`.
2. Permiso declarado ya existente en DB → nunca se crea un duplicado, nunca se modifica automáticamente, **nunca se reactiva automáticamente** si un administrador ya lo desactivó explícitamente. La decisión administrativa siempre prevalece sobre la declaración de arranque.
3. Los permisos declarados no se eliminan físicamente de DB (consistente con `ADR-020`/`INV-AUTH-014`, ya aplicable a `Permission` en general).
4. El `code` de un permiso es estable: la sincronización nunca lo cambia; el ciclo `activate`/`deactivate` administrativo (`SPEC-AUTH-010`) sigue siendo la única forma de cambiar su estado, y `description` solo se edita vía `PATCH /auth/admin/permissions/{name}` (RN-10 SPEC-AUTH-010), nunca por la sincronización.
5. Agregar un permiso de aplicación nuevo requiere solamente agregar un descriptor a la declaración de la aplicación consumidora (un bean `PermissionCatalog`), sin tocar código del módulo Security/Auth.
6. La sincronización no audita cada permiso creado como evento de Security Audit: es un efecto de arranque/despliegue (equivalente a una migración de datos), no una acción de un actor humano identificable — no hay `actor` que registrar de forma significativa. Se decide no introducir un nuevo `SecurityEventType` para esto, por el mismo principio de proporcionalidad que ya rige el catálogo de auditoría (`domain/model/SecurityEventType.java`, Javadoc de la clase).

## 6. Alternativas consideradas

### Alternativa A — Solo el endpoint administrativo (`POST /auth/admin/permissions`)
**Ventajas:** ya existe, no requiere código nuevo.
**Desventajas:** no versionable junto al código de la aplicación consumidora; requiere un paso manual por cada permiso nuevo, en cada entorno, en cada despliegue.

### Alternativa B — Configuración externa (YAML/properties) leída por el propio módulo
**Ventajas:** declarativo sin código Java.
**Desventajas:** el módulo Security/Auth tendría que saber parsear una convención de configuración específica; un bean tipado (`PermissionCatalog`) es más idiomático en Spring y permite que el propio código de la aplicación consumidora sea la única fuente de verdad, sin un archivo de configuración paralelo a mantener sincronizado.

### Alternativa C — Permission Catalog vía beans de Spring (elegida)
**Ventajas:** cada aplicación consumidora declara sus permisos junto a su propio código (`@Configuration`), en el lenguaje que ya usa (Java/Spring), sin que el módulo Security/Auth conozca su significado.
**Desventajas:** requiere que la aplicación consumidora dependa del tipo `PermissionCatalog`/`PermissionDescriptor` expuesto por el módulo — aceptable, es la misma relación de dependencia que ya existe para consumir cualquier Port público de un módulo reutilizable.

## 7. Consecuencias

### Positivas
- El módulo Security/Auth queda genuinamente reutilizable: una aplicación nueva no requiere tocar su código para declarar sus propios permisos.
- La decisión administrativa de desactivar un permiso es duradera: sobrevive a cualquier reinicio o redeploy, incluso si la aplicación consumidora sigue declarándolo (Regla 2).

### Negativas
- Ninguna asignación automática Role↔Permission: declarar un permiso nuevo no lo asocia a ningún Role — eso sigue siendo una decisión administrativa explícita vía `SPEC-AUTH-010` (fuera del alcance de esta ADR, deliberadamente: el módulo no puede inferir qué Role de una aplicación consumidora debería tener qué permiso).

### Riesgos
- Si dos aplicaciones consumidoras distintas declaran el mismo `code` con intenciones distintas, la primera en arrancar "gana" el registro (la segunda lo encuentra ya existente y no lo toca). Mitigado por la convención de namespace (prefijo por aplicación) — no impuesta técnicamente en V1 (ver §8).

## 8. Áreas afectadas
`application.permissioncatalog` (nuevo paquete), `infrastructure.config` (bean del propio módulo), `infrastructure.bootstrap` (runner de sincronización).

## 9. Documentación relacionada
- Requisitos: REQ-AUTH-027 a REQ-AUTH-032 (administración de Permission, `SPEC-AUTH-010`)
- Specifications: SPEC-AUTH-010
- ADR relacionados: ADR-020 (soft deactivation, mismo principio de "nunca reactivar/eliminar automáticamente")
