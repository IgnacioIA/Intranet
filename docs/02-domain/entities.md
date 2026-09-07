# Entidades del Dominio — Módulo de Seguridad (AUTH)

Nota de convención: `.claude/documentation/conventions.md` no define todavía un esquema de identificador para conceptos de dominio (solo para REQ/SPEC/UC/ADR/TEST). Este documento referencia cada concepto por su nombre estable en lugar de un ID codificado. **GAP abierto**, sin impacto bloqueante: si el proyecto adopta un esquema de ID de dominio más adelante, estos nombres deben mapearse, no reemplazarse.

---

# User (Aggregate Root)

## 1. Propósito
Representa a cualquier identidad capaz de autenticarse y operar dentro de la aplicación, sin importar si su origen es local o externo (Active Directory).

## 2. Definición
Un `User` es la raíz de agregado que reúne identidad, estado de cuenta, credenciales (cuando aplica) y las asignaciones de rol vigentes.

## 3. Responsabilidades
- Determinar si puede autenticarse (estado de cuenta).
- Exponer sus roles vigentes y, transitivamente, sus permisos.
- Distinguir roles derivados de Active Directory de roles asignados explícitamente.
- Encapsular su credencial local cuando `provider = LOCAL`.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | Clave propia de la aplicación, estable, generada al crear el `User`. |
| `provider` | `LOCAL` \| `ACTIVE_DIRECTORY` | Origen de la identidad. |
| `externalId` | Identificador externo (nullable si `LOCAL`) | `objectGUID` cuando `provider = ACTIVE_DIRECTORY`. Ver invariante INV-AUTH-002. |
| `username` | Texto | Nombre de usuario/login, mutable, único dentro de su `provider`. |
| `displayName` | Texto | Nombre visible para UI. |
| `email` | Texto (nullable) | Correo de contacto, usado para recuperación de contraseña `LOCAL`. |
| `status` | Estado (ver `states.md`) | Situación actual de la cuenta dentro de la aplicación. |
| `credential` | Value Object `PasswordCredential` (nullable) | Presente únicamente si `provider = LOCAL`. |
| `roles` | Colección de `UserRoleAssignment` | Ver entidad `UserRoleAssignment`. |
| `createdAt` / `lastLoginAt` / `lastAdSyncAt` | Marca de tiempo (UTC) | Metadatos de auditoría del propio agregado. |
| `failedLoginAttempts` | Entero | Contador de intentos fallidos consecutivos de autenticación `LOCAL`, usado por la transición `ACTIVE → LOCKED` (`SPEC-AUTH-001` RN-07). Atributo implícito en la SPEC aprobada; formalizado aquí durante la implementación (Fase 1) sin alterar ninguna decisión. |
| `lockedUntil` | Marca de tiempo (UTC, nullable) | Fin del cooldown de un bloqueo automático (`SPEC-AUTH-001` RN-07, `SPEC-AUTH-009` RN-05). `null` en un bloqueo administrativo manual (`SPEC-AUTH-010`). |

## 5. Invariantes
- `(provider, externalId)` es único cuando `provider = ACTIVE_DIRECTORY`.
- `(provider, username)` es único (la unicidad de `username` es por proveedor, no global).
- `credential` solo puede existir si `provider = LOCAL`.
- Ver `invariants.md` para el detalle completo.

## 6. Estados
Ver `states.md`.

## 7. Transiciones
Ver `transitions.md`.

## 8. Relaciones
- Contiene `UserRoleAssignment` (0..N) → `Role`.
- Emite `RefreshToken` (0..N).
- Origina `SecurityAuditEvent` mediante eventos de dominio/aplicación.

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-006`, `SPEC-AUTH-007`, `SPEC-AUTH-009`, `SPEC-AUTH-010`

## 10. ADR relacionados
- `ADR-003`, `ADR-004`, `ADR-019`

---

# PasswordCredential (Value Object)

## 1. Propósito
Encapsular el hash de la contraseña de un usuario `LOCAL`, evitando que el valor en texto plano exista en cualquier punto salvo el instante de verificación/cambio.

## 2. Definición
Value Object inmutable que contiene el hash Argon2id de la contraseña y los metadatos necesarios para su verificación (parámetros del algoritmo).

## 3. Responsabilidades
- Verificar una contraseña candidata contra el hash almacenado, delegando el algoritmo concreto a un Port de hashing.
- No exponer ni permitir reconstruir el valor original.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `hash` | Texto | Salida de Argon2id (incluye salt y parámetros según formato estándar). |
| `mustChangeOnNextLogin` | Booleano | Verdadero tras bootstrap de un Master Admin o tras un reseteo administrativo. |

## 5. Invariantes
- Nunca se serializa ni se registra en logs/auditoría.

## 6. Estados
No aplica.

## 7. Transiciones
No aplica.

## 8. Relaciones
- Pertenece exclusivamente a un `User` con `provider = LOCAL`.

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-007`, `SPEC-AUTH-009`

## 10. ADR relacionados
- `ADR-011`

---

# Role

## 1. Propósito
Agrupar permisos bajo un nombre significativo para simplificar la asignación de autorización.

## 2. Definición
Entidad que representa un conjunto de `Permission` con un nombre único dentro de la aplicación.

## 3. Responsabilidades
- Contener la colección de permisos que otorga.
- Distinguir si es un rol de sistema (no eliminable), como `ONBOARDING_USER`.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | Clave propia. |
| `name` | Texto único | Ej.: `ONBOARDING_USER`, `MASTER_ADMIN`, `CONTENT_EDITOR`. |
| `description` | Texto | Explicación humana del propósito del rol. |
| `isSystemRole` | Booleano | Verdadero para roles que el sistema requiere para su propio funcionamiento (ej. `ONBOARDING_USER`, `MASTER_ADMIN`) y que no pueden eliminarse ni desactivarse. |
| `active` | Booleano | Cuando es `false`, el rol no otorga ningún permiso a quien lo posea, aunque la asignación (`UserRoleAssignment`) y la relación con sus `Permission` permanezcan intactas (`ADR-020`). No aplica a roles con `isSystemRole = true`, que siempre son `active`. |
| `permissions` | Colección de `Permission` | Permisos otorgados por el rol. |

## 5. Invariantes
- `name` es único.
- Un rol marcado `isSystemRole = true` no puede eliminarse, desactivarse (`active = false`) ni vaciarse de todos sus permisos.
- Un `Role` nunca se elimina físicamente; su ciclo de vida administrativo es `active` ⇄ `inactive` (`ADR-020`), para preservar la integridad histórica de `SecurityAuditEvent` y `UserRoleAssignment`.

## 6. Estados
No aplica una máquina de estados propia; `active` es un atributo booleano, no un ciclo de vida con transiciones condicionadas (ver `ADR-020`).

## 7. Transiciones
No aplica (ver punto 6).

## 8. Relaciones
- Asociado a `User` mediante `UserRoleAssignment`.
- Asociado a `AdGroupRoleMapping` como destino del mapping.
- Contiene `Permission` (N..N).

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-006`, `SPEC-AUTH-008`, `SPEC-AUTH-010`

## 10. ADR relacionados
- `ADR-005`

---

# Permission (Value Object)

## 1. Propósito
Representar, de forma atómica, la capacidad de ejecutar una acción concreta del sistema.

## 2. Definición
Value Object identificado por un nombre siguiendo la convención `RECURSO_ACCION` (ej. `USER_READ`, `USER_MANAGE`).

## 3. Responsabilidades
- Servir como unidad mínima de autorización.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `name` | Texto único | Convención `RECURSO_ACCION`. Inmutable una vez creado. |
| `description` | Texto | Explicación humana. Único campo modificable tras la creación. |
| `isSystemPermission` | Booleano | Verdadero para permisos requeridos por el propio funcionamiento del sistema (ej. `VIEW_ONBOARDING_INFO`, usado por el rol de sistema `ONBOARDING_USER`). No puede desactivarse. |
| `active` | Booleano | Cuando es `false`, el permiso no cuenta en ninguna evaluación de autorización (`SPEC-AUTH-006`), aunque siga asociado a los `Role` que lo incluían (`ADR-020`). No aplica a permisos con `isSystemPermission = true`. |

## 5. Invariantes
- `name` es único e inmutable una vez creado (renombrar un permiso equivale conceptualmente a crear uno nuevo y retirar el anterior, no a editarlo).
- Un permiso con `isSystemPermission = true` no puede desactivarse.
- Un `Permission` nunca se elimina físicamente; su ciclo de vida administrativo es `active` ⇄ `inactive` (`ADR-020`).

## 6. Estados
No aplica una máquina de estados propia; ver punto 5 y `ADR-020`.

## 7. Transiciones
No aplica.

## 8. Relaciones
- Pertenece a uno o más `Role`.

## 9. Specifications relacionadas
- `SPEC-AUTH-006`, `SPEC-AUTH-010`

## 10. ADR relacionados
- `ADR-020`

---

# UserRoleAssignment

## 1. Propósito
Registrar la asociación entre un `User` y un `Role`, junto con su procedencia, para poder distinguir roles derivados de Active Directory de roles asignados explícitamente (REQ-AUTH-007).

## 2. Definición
Entidad de asociación entre `User` y `Role`, con un atributo de procedencia.

## 3. Responsabilidades
- Registrar si el rol proviene de sincronización AD o de asignación administrativa explícita.
- Permitir que un mismo par `(User, Role)` cambie de procedencia (`DERIVED_FROM_AD` → `GRANTED_EXPLICITLY`) sin duplicarse, cuando un administrador otorga explícitamente un rol que el usuario ya poseía por derivación AD (ver INV-AUTH-015, `SPEC-AUTH-010`).

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `userId` | Referencia a `User` | — |
| `roleId` | Referencia a `Role` | — |
| `provenance` | `DERIVED_FROM_AD` \| `GRANTED_EXPLICITLY` | Ver Glosario. |
| `sourceAdGroup` | Texto (nullable) | Grupo AD que originó la asignación, cuando `provenance = DERIVED_FROM_AD`. |
| `assignedAt` | Marca de tiempo (UTC) | — |

## 5. Invariantes
- Un `User` no puede tener el mismo `Role` asignado dos veces (independientemente de la procedencia).
- Solo las asignaciones `DERIVED_FROM_AD` se recalculan en cada login AD exitoso; las `GRANTED_EXPLICITLY` nunca se modifican por ese proceso.

## 6. Estados
No aplica.

## 7. Transiciones
No aplica (se crea/elimina, no transiciona).

## 8. Relaciones
- Une `User` y `Role`.

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-006`, `SPEC-AUTH-010`

## 10. ADR relacionados
Ninguno.

---

# AdGroupRoleMapping

## 1. Propósito
Definir, de forma explícita y administrable, qué grupo de Active Directory otorga qué rol de la aplicación.

## 2. Definición
Entidad que asocia el nombre/identificador de un grupo AD con un `Role` de la aplicación, junto con metadata de auditoría de su propia gestión.

## 3. Responsabilidades
- Ser la única fuente que traduce membresía de grupo AD en autorización de la aplicación.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | — |
| `adGroupIdentifier` | Texto | Nombre o DN del grupo en AD. |
| `roleId` | Referencia a `Role` | Rol otorgado por este grupo. |
| `createdBy` / `updatedBy` | Referencia a `User` (administrador) | Para auditoría de la propia configuración. |
| `createdAt` / `updatedAt` | Marca de tiempo (UTC) | — |

## 5. Invariantes
- `adGroupIdentifier` es único (un grupo AD mapea a un único rol; si se necesitara mapear a varios roles, se modela como varias filas con distinto rol, no ambigüedad de una fila con múltiples destinos).

## 6. Estados
No aplica.

## 7. Transiciones
No aplica.

## 8. Relaciones
- Referencia a `Role`.

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-008`

## 10. ADR relacionados
- `ADR-005`

---

# RefreshToken

## 1. Propósito
Sostener la posibilidad de renovar la sesión sin repetir credenciales, con capacidad de detectar robo mediante rotación.

## 2. Definición
Entidad que representa un token opaco emitido a un `User`, perteneciente a una `RefreshTokenFamily`.

## 3. Responsabilidades
- Determinar si es válido para canjearse por un nuevo par de tokens.
- Señalar reutilización de un token ya revocado.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | — |
| `userId` | Referencia a `User` | — |
| `tokenHash` | Texto | Hash del valor opaco entregado al cliente; nunca el valor en claro. |
| `familyId` | Identificador de familia | Ver `RefreshTokenFamily`. |
| `issuedAt` / `expiresAt` | Marca de tiempo (UTC) | — |
| `revokedAt` | Marca de tiempo (UTC, nullable) | — |
| `replacedByTokenId` | Referencia a `RefreshToken` (nullable) | Token hijo emitido en la rotación. |

## 5. Invariantes
- Un `RefreshToken` revocado nunca vuelve a `revokedAt = null`.
- Un `RefreshToken` usado exitosamente queda revocado y enlazado a su reemplazo en la misma operación (atomicidad, ver INV-AUTH-006).

## 6. Estados
Ver `states.md`.

## 7. Transiciones
Ver `transitions.md`.

## 8. Relaciones
- Pertenece a un `User`.
- Pertenece a una `RefreshTokenFamily`.

## 9. Specifications relacionadas
- `SPEC-AUTH-001`, `SPEC-AUTH-002`, `SPEC-AUTH-003`, `SPEC-AUTH-004`

## 10. ADR relacionados
- `ADR-007`, `ADR-008`, `ADR-010`

---

# RefreshTokenFamily (concepto, no necesariamente tabla propia)

## 1. Propósito
Agrupar la cadena de `RefreshToken` sucesivos de una misma sesión lógica, para poder revocarla completa ante reutilización o ante logout-all.

## 2. Definición
Identificador compartido por todos los `RefreshToken` que descienden, por rotación, de una misma emisión original.

## 3. Responsabilidades
- Servir de unidad de revocación masiva.

## 4. Atributos
No posee atributos propios más allá del identificador compartido por los `RefreshToken` que la componen (puede materializarse como una columna `familyId` en `RefreshToken`, sin tabla independiente).

## 5. Invariantes
- Revocar una familia implica revocar todos los `RefreshToken` vigentes de esa familia.

## 6. Estados
No aplica.

## 7. Transiciones
No aplica.

## 8. Relaciones
- Compuesta por `RefreshToken` (1..N).

## 9. Specifications relacionadas
- `SPEC-AUTH-002`, `SPEC-AUTH-003`, `SPEC-AUTH-004`

## 10. ADR relacionados
- `ADR-008`

---

# PasswordRecoveryToken

## 1. Propósito
Habilitar la confirmación segura de una recuperación de contraseña de un usuario `LOCAL`.

## 2. Definición
Entidad de un solo uso, con expiración corta, que autoriza el establecimiento de una nueva contraseña.

## 3. Responsabilidades
- Garantizar que la confirmación de recuperación solo pueda ejecutarse una vez y dentro de una ventana breve.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | — |
| `userId` | Referencia a `User` | — |
| `tokenHash` | Texto | Hash del valor entregado por correo; nunca el valor en claro. |
| `issuedAt` / `expiresAt` | Marca de tiempo (UTC) | — |
| `usedAt` | Marca de tiempo (UTC, nullable) | — |

## 5. Invariantes
- Un token con `usedAt != null` no puede reutilizarse.
- Solo puede existir asociado a un `User` con `provider = LOCAL`.

## 6. Estados
No aplica (activo/usado/expirado se deriva de sus atributos, no requiere máquina de estados propia).

## 7. Transiciones
No aplica.

## 8. Relaciones
- Pertenece a un `User`.

## 9. Specifications relacionadas
- `SPEC-AUTH-007`

## 10. ADR relacionados
Ninguno.

---

# SecurityAuditEvent

## 1. Propósito
Preservar el registro semántico de eventos de seguridad, distinguido de los logs técnicos (REQ-AUTH-017).

## 2. Definición
Entidad de solo-append que representa un hecho de seguridad ocurrido en el sistema.

## 3. Responsabilidades
- Registrar qué ocurrió, cuándo, sobre quién/por quién, y con qué resultado — nunca datos sensibles.

## 4. Atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | Identificador interno | — |
| `eventType` | Enumerado | Ver catálogo en `docs/03-architecture/security.md`. |
| `occurredAt` | Marca de tiempo (UTC) | — |
| `actorUserId` | Referencia a `User` (nullable) | Nulo en eventos previos a identificar al actor (ej. intento de login fallido con usuario inexistente). |
| `subjectUserId` | Referencia a `User` (nullable) | Usuario afectado, cuando difiere del actor (ej. revocación administrativa). |
| `correlationId` | Texto | Ver REQ-AUTH-024. |
| `outcome` | `SUCCESS` \| `FAILURE` \| `DENIED` | — |
| `metadata` | Estructura clave-valor | Información contextual no sensible (ej. razón de fallo, grupo AD no mapeado). |

## 5. Invariantes
- `metadata` nunca contiene contraseñas, tokens ni secretos (REQ-AUTH-023).
- Un `SecurityAuditEvent` no se modifica ni se elimina una vez creado (append-only).

## 6. Estados
No aplica.

## 7. Transiciones
No aplica.

## 8. Relaciones
- Puede referenciar a `User` (actor y/o sujeto).

## 9. Specifications relacionadas
- Todas las de este módulo.

## 10. ADR relacionados
- `ADR-012`
