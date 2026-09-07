# SPEC-AUTH-010 — Administración de Usuarios, Roles y Permisos

**Estado:** APPROVED
**Versión:** 1.0

## 1. Objetivo
Permitir que un administrador autorizado gestione el modelo de autorización de la aplicación: identidades `LOCAL`, ciclo de vida administrativo de un usuario, catálogo de Roles y Permissions, y la asignación/revocación explícita de Roles (`provenance = GRANTED_EXPLICITLY`), cerrando el único mecanismo productor de esa procedencia que hasta ahora no estaba especificado.

## 2. Alcance

### Incluye
- Consulta de usuarios (LOCAL y AD) con filtros, búsqueda y paginación.
- Alta y actualización de identidad de usuarios `LOCAL`.
- Cambio de estado administrativo de cualquier usuario (enable/disable/lock/unlock/deprovision).
- Alta, modificación y activación/desactivación de Roles.
- Alta, modificación y activación/desactivación de Permissions.
- Asignación y revocación explícita de un Role a un usuario.
- Garantía de continuidad del rol `MASTER_ADMIN` (`INV-AUTH-013`) aplicada a todas las operaciones anteriores.

### No incluye
- Alta manual de usuarios `ACTIVE_DIRECTORY` (se crean únicamente vía login, `SPEC-AUTH-001` UC-AUTH-003).
- Administración del mapping AD Group → Role (ya cubierta por `SPEC-AUTH-008`; esta SPEC solo referencia esa capability, no la duplica).
- Revocación de sesiones (ya cubierta por `SPEC-AUTH-004`; esta SPEC solo referencia esa capability).
- Autenticación, emisión o renovación de tokens (`SPEC-AUTH-001`, `SPEC-AUTH-002`).
- ABAC, policy engines, o cualquier forma de autorización condicional (fuera de alcance del módulo completo).

## 3. Actores
- Administrador con los permisos administrativos correspondientes a cada operación (§9 de este documento).

## 4. Requisitos relacionados
REQ-AUTH-027, REQ-AUTH-028, REQ-AUTH-029, REQ-AUTH-030, REQ-AUTH-031, REQ-AUTH-032, REQ-AUTH-033, REQ-AUTH-034 — y, por extensión, REQ-AUTH-007, REQ-AUTH-015, REQ-AUTH-016.

## 5. Reglas de negocio

- RN-01: El alta manual de un usuario está restringida a `provider = LOCAL`. No es posible crear manualmente un `User` `ACTIVE_DIRECTORY`.
- RN-02: La actualización de identidad (`email`, `displayName`) solo aplica a usuarios `LOCAL`. Los atributos de un usuario AD se sincronizan desde AD (`SPEC-AUTH-001`) y no son editables administrativamente.
- RN-03: Los cambios de estado administrativo deben respetar las transiciones válidas definidas en `02-domain/transitions.md`; una transición no contemplada se rechaza.
- RN-04: Deshabilitar, bloquear o deprovisionar a un usuario revoca inmediatamente todas sus sesiones activas (efecto colateral ya definido en `02-domain/transitions.md`).
- RN-05: Reactivar un usuario reevalúa sus roles vigentes (derivados + explícitos) para determinar si el estado resultante es `ACTIVE` o `PENDING_ONBOARDING`; nunca asume `ACTIVE` ciegamente.
- RN-06: `DEPROVISIONED` es terminal; esta SPEC no define ninguna transición de salida de ese estado.
- RN-07: Toda operación de esta SPEC que afecte el estado o los roles de un usuario debe verificar `INV-AUTH-013` (continuidad de `MASTER_ADMIN`) antes de aplicarse; si la violaría, se rechaza mediante una validación transaccional y bloqueante — nunca una alerta posterior.
- RN-08: `Role` y `Permission` nunca se eliminan físicamente; su retiro se expresa mediante `active = false` (`ADR-020`).
- RN-09: Un `Role`/`Permission` de sistema (`isSystemRole`/`isSystemPermission = true`) no puede desactivarse.
- RN-10: El nombre de un `Permission` es inmutable una vez creado; solo su descripción es editable (ver `entities.md`).
- RN-11: Otorgar explícitamente a un usuario un `Role` que ya posee con `provenance = DERIVED_FROM_AD` actualiza (*upgrade*) la fila existente a `GRANTED_EXPLICITLY`, sin duplicarla (`INV-AUTH-015`).
- RN-12: Otorgar explícitamente un `Role` que el usuario ya posee con `provenance = GRANTED_EXPLICITLY` es idempotente: no genera una segunda fila ni un error, aunque sí se audita como confirmación explícita.
- RN-13: Revocar una asignación `GRANTED_EXPLICITLY` sobre un `Role` que el usuario también derivaría de AD no impide que, en su próximo login AD, el `Role` vuelva a asignarse como `DERIVED_FROM_AD` si el mapping sigue vigente. Este es el comportamiento esperado, no un error.
- RN-14: Un intento de revocar "explícitamente" una asignación que es puramente `DERIVED_FROM_AD` se rechaza, orientando al mecanismo correcto (modificar el mapping en `SPEC-AUTH-008`, o esperar la próxima sincronización AD).
- RN-15: Revocar el último `Role` vigente de un usuario lo transiciona a `PENDING_ONBOARDING` (no es un estado de error).
- RN-16: Toda operación de esta SPEC audita tanto `AUTHORIZATION_DENIED` como `AUTHORIZATION_GRANTED`, por ser operaciones administrativas sensibles (política ya establecida en `docs/03-architecture/security.md §5`).

## 6. Casos de uso

### UC-AUTH-015 — Consultar usuarios

**Actor:** Administrador con `USER_READ`.
**Precondiciones:** Ninguna adicional a la autenticación y el permiso.
**Flujo principal:**
1. El administrador solicita la lista de usuarios, con filtros opcionales (`status`, `provider`, texto de búsqueda) y paginación.
2. El sistema devuelve la página solicitada con la información básica de cada usuario (sin datos sensibles).
3. El administrador puede solicitar el detalle de un usuario puntual, incluyendo sus roles vigentes con procedencia.

**Flujos alternativos:** ninguno relevante.
**Errores:** permiso insuficiente (403), usuario inexistente en consulta de detalle (404).
**Resultado esperado:** visibilidad completa del estado del modelo de usuarios sin exponer credenciales ni tokens.

---

### UC-AUTH-016 — Crear usuario LOCAL

**Actor:** Administrador con `USER_MANAGE`.
**Precondiciones:** El `username` propuesto no colisiona con otro `User` `LOCAL` existente (INV-AUTH-001).
**Flujo principal:**
1. El administrador envía `username`, `email`, `displayName` y una contraseña inicial (o el sistema genera una).
2. El sistema crea el `User(provider=LOCAL)` con `credential` (Argon2id, `ADR-011`) y `mustChangeOnNextLogin = true`, siguiendo el mismo patrón de bootstrap ya aprobado en `SPEC-AUTH-009` para credenciales iniciales.
3. El sistema audita `USER_CREATED`.

**Flujos alternativos:**
- 1a. `username` ya existe para `provider = LOCAL` → 409 Conflict.

**Errores:** permiso insuficiente, `username` duplicado, contraseña inicial que no cumple la política vigente (`SPEC-AUTH-007`).
**Resultado esperado:** nueva identidad `LOCAL`, sin rol alguno por defecto (queda en `PENDING_ONBOARDING` hasta que se le asigne un `Role` — UC-AUTH-021 — o se le otorgue mediante el mecanismo que corresponda).

---

### UC-AUTH-017 — Actualizar identidad de un usuario LOCAL

**Actor:** Administrador con `USER_MANAGE`.
**Precondiciones:** El usuario objetivo tiene `provider = LOCAL` (RN-02).
**Flujo principal:**
1. El administrador envía los campos a actualizar (`email`, `displayName`).
2. El sistema actualiza el `User`.
3. El sistema audita `USER_UPDATED`.

**Flujos alternativos:**
- 1a. Usuario objetivo con `provider = ACTIVE_DIRECTORY` → rechazado (RN-02).

**Errores:** permiso insuficiente, usuario inexistente, usuario AD (400).
**Resultado esperado:** identidad `LOCAL` actualizada.

---

### UC-AUTH-018 — Cambiar estado administrativo de un usuario

**Actor:** Administrador con `USER_MANAGE`.
**Precondiciones:** La transición solicitada es válida según `02-domain/transitions.md` (RN-03).
**Flujo principal (para cada operación: enable, disable, lock, unlock, deprovision):**
1. El administrador solicita la transición sobre un usuario objetivo.
2. El sistema verifica que la transición sea válida desde el estado actual (RN-03).
3. El sistema verifica `INV-AUTH-013` cuando la operación es `disable`, `lock` o `deprovision` (RN-07): si el usuario objetivo es el único `User` `ACTIVE` con el rol `MASTER_ADMIN`, la operación se rechaza.
4. El sistema aplica la transición; si corresponde (`disable`/`lock`/`deprovision`), revoca todas las sesiones activas del usuario (RN-04).
5. Si la operación es `enable` (reactivación), el sistema reevalúa los roles vigentes del usuario para determinar si el estado resultante es `ACTIVE` o `PENDING_ONBOARDING` (RN-05).
6. El sistema audita el evento correspondiente (`USER_ENABLED` / `USER_DISABLED` / `USER_LOCKED` / `USER_UNLOCKED` / `USER_DEPROVISIONED`).

**Flujos alternativos:**
- 3a. La operación violaría `INV-AUTH-013` → rechazada con 409, sin aplicar ningún cambio.
- Transición no válida desde el estado actual (ej. intentar `enable` sobre un usuario `DEPROVISIONED`) → 409.

**Errores:** permiso insuficiente, usuario inexistente, transición inválida, violación de continuidad de `MASTER_ADMIN`.
**Resultado esperado:** el usuario queda en el estado solicitado, con efecto inmediato sobre su capacidad de autenticarse y operar, salvo que la operación hubiera dejado al sistema sin `MASTER_ADMIN`.

---

### UC-AUTH-019 — Administrar Roles

**Actor:** Administrador con `ROLE_MANAGE` (lectura: `ROLE_READ`).
**Precondiciones:** Para alta, el `name` propuesto no existe. Para desactivación, el `Role` no es de sistema (RN-09) ni es la única vía de continuidad de `MASTER_ADMIN` (RN-07).
**Flujo principal (alta):**
1. El administrador envía `name`, `description` y, opcionalmente, el conjunto inicial de `Permission`.
2. El sistema crea el `Role` con `active = true`.
3. El sistema audita `ROLE_CREATED`.

**Flujos alternativos:**
- Modificación: el administrador actualiza `description` y/o el conjunto de `Permission` → se actualiza y se audita `ROLE_UPDATED`.
- Desactivación: el administrador desactiva un `Role` → `active = false`; se audita `ROLE_DISABLED`. Rechazada si `isSystemRole = true` o si es `MASTER_ADMIN` y su desactivación violaría `INV-AUTH-013`.
- Reactivación: el administrador reactiva un `Role` inactivo → `active = true`; se audita `ROLE_UPDATED`.
- `name` duplicado en alta → 409 Conflict.

**Errores:** permiso insuficiente, `name` duplicado, `Role` inexistente, desactivación de rol de sistema o protegido por continuidad.
**Resultado esperado:** el catálogo de Roles refleja las decisiones administrativas, sin pérdida de integridad histórica.

---

### UC-AUTH-020 — Administrar Permissions

**Actor:** Administrador con `PERMISSION_MANAGE` (lectura: `PERMISSION_READ`).
**Precondiciones:** Para alta, el `name` propuesto no existe. Para desactivación, la `Permission` no es de sistema (RN-09).
**Flujo principal (alta):**
1. El administrador envía `name` (convención `RECURSO_ACCION`) y `description`.
2. El sistema crea la `Permission` con `active = true`.
3. El sistema audita `PERMISSION_CREATED`.

**Flujos alternativos:**
- Modificación: el administrador actualiza únicamente `description` (RN-10) → se audita `PERMISSION_UPDATED`.
- Desactivación/reactivación: análogas a Role, auditadas de igual forma. Rechazada si `isSystemPermission = true`.
- `name` duplicado en alta → 409 Conflict.

**Errores:** permiso insuficiente, `name` duplicado, intento de modificar `name`, `Permission` inexistente, desactivación de permiso de sistema.
**Resultado esperado:** el catálogo de Permissions refleja las decisiones administrativas, sin pérdida de integridad histórica.

---

### UC-AUTH-021 — Asignar Role explícito a un usuario

**Actor:** Administrador con `ROLE_ASSIGN`.
**Precondiciones:** El `Role` objetivo existe y está `active`.
**Flujo principal:**
1. El administrador solicita asignar un `Role` a un usuario.
2. El sistema verifica si ya existe un `UserRoleAssignment` para ese par `(usuario, Role)`.
3. Si no existe → crea la asignación con `provenance = GRANTED_EXPLICITLY`.
4. Si existe con `provenance = DERIVED_FROM_AD` → actualiza esa fila a `GRANTED_EXPLICITLY` (RN-11, INV-AUTH-015).
5. Si existe con `provenance = GRANTED_EXPLICITLY` → no hace nada (idempotente, RN-12).
6. Si el usuario estaba en `PENDING_ONBOARDING` y esta es su primera asignación vigente → transiciona a `ACTIVE`.
7. El sistema audita `ROLE_ASSIGNED`.

**Flujos alternativos:**
- El `Role` está `active = false` → rechazado (400): no se puede asignar un rol inactivo.

**Errores:** permiso insuficiente, usuario o rol inexistente, rol inactivo.
**Resultado esperado:** el usuario posee el `Role` con `provenance = GRANTED_EXPLICITLY`, de forma idempotente y sin duplicar asignaciones.

---

### UC-AUTH-022 — Revocar Role explícito de un usuario

**Actor:** Administrador con `ROLE_REVOKE`.
**Precondiciones:** Existe un `UserRoleAssignment(usuario, Role)` con `provenance = GRANTED_EXPLICITLY`.
**Flujo principal:**
1. El administrador solicita revocar un `Role` de un usuario.
2. El sistema verifica `INV-AUTH-013` si el `Role` es `MASTER_ADMIN` (RN-07): rechaza si el usuario es el único `ACTIVE` que lo posee.
3. El sistema elimina el `UserRoleAssignment`.
4. Si el usuario queda sin ningún `Role` vigente → transiciona a `PENDING_ONBOARDING` (RN-15).
5. El sistema audita `ROLE_REVOKED`.

**Flujos alternativos:**
- La asignación existente es `DERIVED_FROM_AD` (no `GRANTED_EXPLICITLY`) → rechazada (RN-14), con un error que orienta a `SPEC-AUTH-008` o a esperar la próxima sincronización.
- La revocación violaría `INV-AUTH-013` → rechazada (409).

**Errores:** permiso insuficiente, asignación inexistente, asignación de procedencia AD, violación de continuidad de `MASTER_ADMIN`.
**Resultado esperado:** la asignación explícita se retira; si el `Role` seguía siendo válido por AD, reaparecerá como `DERIVED_FROM_AD` en el siguiente login (RN-13), lo cual es intencional.

## 7. Casos límite
- Un `Role` es desactivado mientras varios usuarios lo poseen → pierden sus permisos de forma inmediata (RN-08, `ADR-020`); las asignaciones no se eliminan, solo dejan de tener efecto.
- Se intenta asignar el `Role` `MASTER_ADMIN` a un segundo usuario mientras el primero sigue activo → permitido (no hay límite superior de administradores, solo un mínimo garantizado — INV-AUTH-013 protege el piso, no el techo).
- Se intenta desactivar `ROLE_MANAGE` o `USER_MANAGE` como `Permission` mientras es la única vía de un administrador para seguir operando (fuera del caso ya cubierto por `MASTER_ADMIN`) → **no está protegido por un invariante formal más allá de la protección de `MASTER_ADMIN`**; se documenta como riesgo operativo conocido, no como comportamiento indefinido: un administrador con criterio puede autolimitarse, pero el sistema no lo impide salvo para el caso específico de `MASTER_ADMIN` (alcance deliberadamente acotado, ver §19 de la tarea de cierre — evitar sobre-ingeniería de protección generalizada de permisos administrativos).
- Alta de un usuario `LOCAL` con el mismo `username` que un usuario `ACTIVE_DIRECTORY` existente → permitido (INV-AUTH-001: unicidad por proveedor, no global).

## 8. Criterios de aceptación

```gherkin
Feature: Administración de usuarios

  Scenario: Admin consulta usuarios
    Given un administrador con permiso USER_READ
    When solicita la lista de usuarios
    Then recibe una página de resultados sin datos sensibles

  Scenario: Admin desactiva un usuario
    Given un administrador con permiso USER_MANAGE
    And un usuario ACTIVE que no es el único MASTER_ADMIN
    When lo deshabilita
    Then el usuario pasa a DISABLED
    And todas sus sesiones activas quedan revocadas
    And se registra un evento USER_DISABLED

  Scenario: Usuario deshabilitado no puede acceder
    Given un usuario DISABLED con un Access Token todavía no expirado
    When realiza una solicitud protegida
    Then la solicitud se deniega

  Scenario: Rechazo al intentar deshabilitar al último Master Admin
    Given que existe exactamente un User ACTIVE con el rol MASTER_ADMIN
    When un administrador intenta deshabilitarlo
    Then la operación se rechaza con 409
    And no se aplica ningún cambio
    And se registra un evento AUTHORIZATION_DENIED o equivalente de rechazo

Feature: Administración de Roles

  Scenario: Crear un Role
    Given un administrador con permiso ROLE_MANAGE
    When crea un Role "CONTENT_EDITOR" con una descripción
    Then el Role queda creado y activo
    And se registra un evento ROLE_CREATED

  Scenario: Modificar un Role
    Given un Role existente
    When un administrador actualiza su conjunto de Permissions
    Then el cambio queda reflejado
    And se registra un evento ROLE_UPDATED

  Scenario: Asignar un Role a un usuario
    Given un Role activo y un usuario existente
    When un administrador con ROLE_ASSIGN asigna ese Role al usuario
    Then el usuario posee el Role con provenance GRANTED_EXPLICITLY
    And se registra un evento ROLE_ASSIGNED

  Scenario: Revocar un Role de un usuario
    Given un usuario con un Role GRANTED_EXPLICITLY
    When un administrador con ROLE_REVOKE lo revoca
    Then la asignación se elimina
    And se registra un evento ROLE_REVOKED

  Scenario: Role inexistente
    Given que no existe un Role con el identificador indicado
    When un administrador intenta consultarlo o asignarlo
    Then el sistema responde 404

  Scenario: Role duplicado
    Given un Role existente con name "CONTENT_EDITOR"
    When un administrador intenta crear otro Role con el mismo name
    Then el sistema rechaza la operación con 409

Feature: Administración de Permissions

  Scenario: Crear una Permission
    Given un administrador con permiso PERMISSION_MANAGE
    When crea una Permission "CONTENT_PUBLISH"
    Then la Permission queda creada y activa
    And se registra un evento PERMISSION_CREATED

  Scenario: Asociar una Permission a un Role
    Given una Permission activa y un Role existente
    When un administrador la agrega al conjunto de permisos del Role
    Then el Role queda actualizado
    And se registra un evento ROLE_UPDATED

  Scenario: Permission inexistente
    Given que no existe una Permission con el identificador indicado
    When un administrador intenta consultarla o asociarla a un Role
    Then el sistema responde 404

  Scenario: Permission duplicada
    Given una Permission existente con name "CONTENT_PUBLISH"
    When un administrador intenta crear otra Permission con el mismo name
    Then el sistema rechaza la operación con 409

Feature: Autorización con efecto inmediato

  Scenario: Role agregado habilita la siguiente request
    Given un usuario sin el permiso requerido por una acción
    When un administrador le asigna un Role que incluye ese permiso
    And el usuario repite la solicitud sin obtener un nuevo Access Token
    Then la solicitud se autoriza

  Scenario: Role revocado deniega la siguiente request
    Given un usuario autorizado para una acción mediante un Role explícito
    When un administrador le revoca ese Role
    And el usuario repite la solicitud sin obtener un nuevo Access Token
    Then la solicitud se deniega

  Scenario: Permission desactivada deniega la siguiente request
    Given un usuario autorizado para una acción mediante un Permission activo
    When un administrador desactiva ese Permission
    And el usuario repite la solicitud
    Then la solicitud se deniega

Feature: Coexistencia con Active Directory

  Scenario: Rol explícito sobrevive a una sincronización AD
    Given un usuario AD con un Role otorgado explícitamente
    And ese Role no corresponde a ningún grupo AD del usuario
    When el usuario inicia sesión nuevamente contra AD
    Then el Role explícito permanece asignado sin cambios

  Scenario: Asignación explícita sobre un Role ya derivado se convierte (upgrade)
    Given un usuario AD con un Role DERIVED_FROM_AD
    When un administrador asigna explícitamente ese mismo Role al usuario
    Then la asignación existente pasa a provenance GRANTED_EXPLICITLY
    And no se crea una segunda asignación para el mismo Role

  Scenario: Rechazo al revocar explícitamente un Role puramente derivado
    Given un usuario AD con un Role únicamente DERIVED_FROM_AD
    When un administrador intenta revocarlo explícitamente
    Then la operación se rechaza
    And el sistema orienta a modificar el mapping AD correspondiente

  Scenario: Rol derivado reaparece tras revocar la asignación explícita
    Given un usuario AD cuyo Role fue upgradeado a GRANTED_EXPLICITLY
    And el mapping AD que originalmente lo derivaba sigue vigente
    When un administrador revoca la asignación explícita
    And el usuario vuelve a iniciar sesión contra AD
    Then el Role reaparece con provenance DERIVED_FROM_AD

Feature: Continuidad del Master Admin

  Scenario: Protección transaccional del último Master Admin
    Given que existe exactamente un User ACTIVE con el rol MASTER_ADMIN
    When se intenta, mediante cualquier operación de esta SPEC, dejarlo sin ese rol o sin acceso
    Then la operación se rechaza

  Scenario: Intento concurrente de remover al último Master Admin
    Given que existe exactamente un User ACTIVE con el rol MASTER_ADMIN
    When dos administradores distintos intentan, simultáneamente, deshabilitarlo y revocarle el rol MASTER_ADMIN
    Then como máximo una de las dos operaciones podría aplicarse si dejara de ser "la última" por la otra vía
    And el invariante INV-AUTH-013 nunca queda violado al finalizar ambas operaciones
```

## 9. Dependencias
`UserRepositoryPort`, `PasswordHasherPort`, `AuditPort`. Depende conceptualmente de `SPEC-AUTH-001` (roles derivados y estados), `SPEC-AUTH-006` (evaluación de permisos), `SPEC-AUTH-008` (mapping AD, no duplicado), `SPEC-AUTH-004` (revocación de sesiones, no duplicada), `SPEC-AUTH-009` (Master Admin, formalizado aquí como Role `MASTER_ADMIN`).

## 10. Restricciones
No implementar ABAC, ownership ni autorización condicional (fuera de alcance del módulo). No introducir un permiso por cada combinación CRUD×recurso: se consolida lectura/escritura en `_READ`/`_MANAGE` salvo `ROLE_ASSIGN`/`ROLE_REVOKE`, que se mantienen separados por su asimetría de riesgo (otorgar acceso vs. retirarlo).

## 11. Seguridad
Todas las operaciones de esta SPEC son administrativas y sensibles: se audita tanto `AUTHORIZATION_GRANTED` como `AUTHORIZATION_DENIED` (RN-16). La protección de `MASTER_ADMIN` (RN-07, INV-AUTH-013) es la única garantía formal contra auto-bloqueo administrativo; no se generaliza a todos los permisos administrativos (ver Casos límite, §7) para evitar sobre-ingeniería no solicitada.

## 12. API Contract

### Usuarios

| Método | Endpoint | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/auth/admin/users` | `USER_READ` | Lista paginada, filtros `status`, `provider`, `q` (texto). |
| `GET` | `/auth/admin/users/{userId}` | `USER_READ` | Detalle, incluye roles con procedencia. |
| `POST` | `/auth/admin/users` | `USER_MANAGE` | Crea usuario `LOCAL` (UC-AUTH-016). |
| `PATCH` | `/auth/admin/users/{userId}` | `USER_MANAGE` | Actualiza identidad `LOCAL` (UC-AUTH-017). |
| `POST` | `/auth/admin/users/{userId}/enable` | `USER_MANAGE` | UC-AUTH-018. |
| `POST` | `/auth/admin/users/{userId}/disable` | `USER_MANAGE` | UC-AUTH-018. |
| `POST` | `/auth/admin/users/{userId}/lock` | `USER_MANAGE` | UC-AUTH-018. |
| `POST` | `/auth/admin/users/{userId}/unlock` | `USER_MANAGE` | UC-AUTH-018. |
| `POST` | `/auth/admin/users/{userId}/deprovision` | `USER_MANAGE` | UC-AUTH-018 (terminal). |

**Request — `POST /auth/admin/users`:**
```json
{ "username": "jperez", "email": "jperez@example.com", "displayName": "Juan Pérez", "initialPassword": "string opcional" }
```
**Response (201):**
```json
{ "id": "u-123", "provider": "LOCAL", "username": "jperez", "status": "PENDING_ONBOARDING", "mustChangeOnNextLogin": true }
```
**Request — `PATCH /auth/admin/users/{userId}`:**
```json
{ "email": "nuevo@example.com", "displayName": "Nuevo Nombre" }
```
**Response — cambios de estado (`.../enable`, `.../disable`, etc.):**
```json
{ "id": "u-123", "status": "DISABLED" }
```

**Códigos HTTP (comunes a Usuarios):** `200`/`201` OK · `400` provider AD en operación restringida a LOCAL, o transición inválida · `403` permiso insuficiente · `404` usuario inexistente · `409` `username` duplicado (alta), o violación de `INV-AUTH-013` (cambios de estado sobre el único `MASTER_ADMIN`).

**Errores (RFC 7807):** `insufficient-permissions` (403) · `user-not-found` (404) · `username-already-exists` (409) · `identity-managed-externally` (400, usuario AD en operación LOCAL-only) · `invalid-state-transition` (400) · `master-admin-continuity-violation` (409).

### Roles

| Método | Endpoint | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/auth/admin/roles` | `ROLE_READ` | Lista. |
| `GET` | `/auth/admin/roles/{roleId}` | `ROLE_READ` | Detalle, incluye Permissions. |
| `POST` | `/auth/admin/roles` | `ROLE_MANAGE` | Alta. |
| `PUT` | `/auth/admin/roles/{roleId}` | `ROLE_MANAGE` | Modifica descripción/permisos. |
| `POST` | `/auth/admin/roles/{roleId}/activate` | `ROLE_MANAGE` | — |
| `POST` | `/auth/admin/roles/{roleId}/deactivate` | `ROLE_MANAGE` | Rechazada si `isSystemRole` o viola `INV-AUTH-013`. |

**Request — `POST`/`PUT`:**
```json
{ "name": "CONTENT_EDITOR", "description": "Edición de contenido", "permissions": ["CONTENT_READ", "CONTENT_CREATE"] }
```
**Response:**
```json
{ "id": "role-content-editor", "name": "CONTENT_EDITOR", "description": "Edición de contenido", "isSystemRole": false, "active": true, "permissions": ["CONTENT_READ", "CONTENT_CREATE"] }
```
**Códigos HTTP:** `200`/`201` OK · `403` permiso insuficiente · `404` Role inexistente · `409` `name` duplicado (alta), o desactivación de rol de sistema/protegido.
**Errores (RFC 7807):** `insufficient-permissions` · `role-not-found` · `role-already-exists` · `system-role-protected` (400/409) · `master-admin-continuity-violation` (409, al desactivar `MASTER_ADMIN`).

### Permissions

| Método | Endpoint | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/auth/admin/permissions` | `PERMISSION_READ` | Lista. |
| `GET` | `/auth/admin/permissions/{permissionId}` | `PERMISSION_READ` | Detalle. |
| `POST` | `/auth/admin/permissions` | `PERMISSION_MANAGE` | Alta. |
| `PATCH` | `/auth/admin/permissions/{permissionId}` | `PERMISSION_MANAGE` | Solo `description` (RN-10). |
| `POST` | `/auth/admin/permissions/{permissionId}/activate` | `PERMISSION_MANAGE` | — |
| `POST` | `/auth/admin/permissions/{permissionId}/deactivate` | `PERMISSION_MANAGE` | Rechazada si `isSystemPermission`. |

**Request — `POST`:**
```json
{ "name": "CONTENT_PUBLISH", "description": "Publicar contenido" }
```
**Response:**
```json
{ "id": "perm-content-publish", "name": "CONTENT_PUBLISH", "description": "Publicar contenido", "isSystemPermission": false, "active": true }
```
**Códigos HTTP:** `200`/`201` OK · `400` intento de modificar `name` · `403` permiso insuficiente · `404` Permission inexistente · `409` `name` duplicado (alta), o desactivación de permiso de sistema.
**Errores (RFC 7807):** `insufficient-permissions` · `permission-not-found` · `permission-already-exists` · `permission-name-immutable` · `system-permission-protected`.

### Asignación de Roles a Usuarios

| Método | Endpoint | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/auth/admin/users/{userId}/roles` | `USER_READ` | Lista roles del usuario, con `provenance`. |
| `POST` | `/auth/admin/users/{userId}/roles` | `ROLE_ASSIGN` | UC-AUTH-021. |
| `DELETE` | `/auth/admin/users/{userId}/roles/{roleId}` | `ROLE_REVOKE` | UC-AUTH-022. |

**Request — `POST .../roles`:**
```json
{ "roleId": "role-content-editor" }
```
**Response (200/201):**
```json
{ "userId": "u-123", "roleId": "role-content-editor", "provenance": "GRANTED_EXPLICITLY" }
```
**Response — `GET .../roles`:**
```json
[
  { "roleId": "role-onboarding", "provenance": "DERIVED_FROM_AD", "sourceAdGroup": null },
  { "roleId": "role-content-editor", "provenance": "GRANTED_EXPLICITLY" }
]
```
**Códigos HTTP:** `200`/`201` OK (asignación, incluyendo el caso idempotente de RN-12) · `400` Role inactivo · `403` permiso insuficiente · `404` usuario/rol/asignación inexistente · `409` revocación sobre asignación `DERIVED_FROM_AD` (RN-14), o violación de `INV-AUTH-013` al revocar `MASTER_ADMIN`.
**Errores (RFC 7807):** `insufficient-permissions` · `user-not-found` · `role-not-found` · `role-inactive` · `assignment-not-explicit` (409, RN-14) · `master-admin-continuity-violation` (409).

### Referencia a capabilities existentes (no duplicadas)
- Administración del mapping AD Group → Role: ver `SPEC-AUTH-008` (`/auth/admin/ad-group-mappings`).
- Revocación de sesiones de un usuario: ver `SPEC-AUTH-004` (`/auth/admin/users/{userId}/revoke-sessions`).

## 13. Referencias de dominio
- Entidad: User, Role, Permission, UserRoleAssignment
- Invariante: INV-AUTH-001, INV-AUTH-004, INV-AUTH-005, INV-AUTH-011, INV-AUTH-013, INV-AUTH-014, INV-AUTH-015
- Estado: PENDING_ONBOARDING, ACTIVE, LOCKED, DISABLED, DEPROVISIONED; `Role.active`/`Permission.active`
- Transición: ver `02-domain/transitions.md`

## 14. Trazabilidad
- Requisitos: REQ-AUTH-027, REQ-AUTH-028, REQ-AUTH-029, REQ-AUTH-030, REQ-AUTH-031, REQ-AUTH-032, REQ-AUTH-033, REQ-AUTH-034
- Dominio: User, Role, Permission, UserRoleAssignment
- Pruebas: TEST-AUTH-0NN (a asignar durante implementación, ver `testing-strategy.md`)
- ADR: ADR-006, ADR-011, ADR-019, ADR-020

## 15. GAPs
Sin GAPs bloqueantes. Se documenta explícitamente en §7 (Casos límite) que la protección transaccional de continuidad administrativa se limita a `MASTER_ADMIN` (INV-AUTH-013) y no se generaliza a otros permisos administrativos — esto es una decisión de alcance, no una omisión.
