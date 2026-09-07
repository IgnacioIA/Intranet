# Invariantes del Dominio — Módulo de Seguridad (AUTH)

Cada invariante posee un identificador local `INV-AUTH-NNN` (referencia interna de este documento; no forma parte del esquema de identificadores de `.claude/documentation/conventions.md`, que no cubre invariantes — **GAP** de convención, sin impacto bloqueante).

---

## INV-AUTH-001 — Unicidad de identidad por proveedor
`(provider, username)` es único. La unicidad de `username` es por proveedor: un `User` `LOCAL` y un `User` `ACTIVE_DIRECTORY` pueden compartir el mismo `username` sin colisionar.

**Afecta:** `User`
**Relacionado con:** `SPEC-AUTH-001`, REQ-AUTH-001, REQ-AUTH-002

---

## INV-AUTH-002 — Unicidad de identidad externa
`(provider, externalId)` es único cuando `provider = ACTIVE_DIRECTORY`. `externalId` es el `objectGUID`, nunca el `username` ni el UPN.

**Afecta:** `User`
**Relacionado con:** `SPEC-AUTH-001`, REQ-AUTH-003

---

## INV-AUTH-003 — Credencial exclusiva de usuarios locales
Un `User` posee `credential` (`PasswordCredential`) si y solo si `provider = LOCAL`. Un `User` `ACTIVE_DIRECTORY` nunca almacena contraseña, hash ni ningún derivado de ella.

**Afecta:** `User`, `PasswordCredential`
**Relacionado con:** REQ-AUTH-012

---

## INV-AUTH-004 — Un usuario sin roles válidos no tiene acceso general
Un `User` cuyo conjunto de `UserRoleAssignment` esté vacío, o cuyos roles derivados de AD hayan quedado en cero tras la última sincronización, debe encontrarse en estado `PENDING_ONBOARDING` con únicamente el rol `ONBOARDING_USER`. No existe un estado intermedio de "acceso de lectura general" fuera del modelo de permisos.

**Afecta:** `User`, `Role`
**Relacionado con:** `SPEC-AUTH-001`, REQ-AUTH-005

---

## INV-AUTH-005 — Los roles derivados de AD no sobreviven a su ausencia de mapping
Tras cada sincronización AD exitosa, todo `UserRoleAssignment` con `provenance = DERIVED_FROM_AD` que ya no corresponda a un grupo AD vigente del usuario debe eliminarse. Los `UserRoleAssignment` con `provenance = GRANTED_EXPLICITLY` nunca se ven afectados por esta sincronización.

**Afecta:** `UserRoleAssignment`
**Relacionado con:** `SPEC-AUTH-001`, `SPEC-AUTH-006`, REQ-AUTH-007

---

## INV-AUTH-006 — Atomicidad de la rotación de Refresh Token
La revocación del `RefreshToken` presentado y la creación de su reemplazo deben ocurrir en una única operación atómica. No debe existir una ventana observable en la que ambos estén simultáneamente válidos, ni una en la que ninguno lo esté.

**Afecta:** `RefreshToken`
**Relacionado con:** `SPEC-AUTH-002`, REQ-AUTH-009

---

## INV-AUTH-007 — Reutilización de Refresh Token revoca la familia completa
Si se presenta un `RefreshToken` cuyo `revokedAt` ya está establecido, deben revocarse inmediatamente todos los `RefreshToken` vigentes de su `RefreshTokenFamily`, y el evento debe registrarse en `SecurityAuditEvent` con `outcome = DENIED` y severidad alta.

**Afecta:** `RefreshToken`, `RefreshTokenFamily`
**Relacionado con:** `SPEC-AUTH-002`, REQ-AUTH-009

---

## INV-AUTH-008 — Un token de recuperación de contraseña es de un solo uso
Un `PasswordRecoveryToken` con `usedAt != null` no puede volver a autorizar el establecimiento de una nueva contraseña, independientemente de si aún no expiró.

**Afecta:** `PasswordRecoveryToken`
**Relacionado con:** `SPEC-AUTH-007`, REQ-AUTH-013

---

## INV-AUTH-009 — Ningún dato sensible en auditoría o logs
Ningún `SecurityAuditEvent.metadata`, ni ninguna línea de log técnico, puede contener: contraseñas, Access Tokens, Refresh Tokens completos, secretos de firma o credenciales de Active Directory.

**Afecta:** `SecurityAuditEvent`
**Relacionado con:** REQ-AUTH-023

---

## INV-AUTH-010 — Un grupo AD mapea a exactamente un rol por fila de mapping
`AdGroupRoleMapping.adGroupIdentifier` es único. Si un grupo debe otorgar múltiples roles, se representa mediante múltiples filas con distinto `roleId`, nunca mediante una fila con destino ambiguo.

**Afecta:** `AdGroupRoleMapping`
**Relacionado con:** `SPEC-AUTH-008`, REQ-AUTH-004

---

## INV-AUTH-011 — Un rol de sistema no puede quedar sin efecto
Un `Role` con `isSystemRole = true` (ej. `ONBOARDING_USER`) no puede eliminarse ni quedar sin permisos, porque el correcto funcionamiento del flujo de onboarding depende de su existencia.

**Afecta:** `Role`
**Relacionado con:** `SPEC-AUTH-001`, REQ-AUTH-005

---

## INV-AUTH-012 — La autorización nunca se evalúa exclusivamente desde el token
Ninguna decisión de autorización puede basarse únicamente en los claims de roles/permisos embebidos en el Access Token sin contrastarlos, en el momento del request, contra el estado vigente en base de datos.

**Afecta:** `User`, `UserRoleAssignment`
**Relacionado con:** `SPEC-AUTH-006`, REQ-AUTH-015

---

## INV-AUTH-013 — Continuidad del rol `MASTER_ADMIN`
En todo momento debe existir al menos un `User` `LOCAL` con `status = ACTIVE` que posea el rol de sistema `MASTER_ADMIN` con `provenance = GRANTED_EXPLICITLY`. Toda operación administrativa que resultaría en violar este invariante (deshabilitar, bloquear, deprovisionar o revocar el rol del único `User` que lo cumple; desactivar el propio rol `MASTER_ADMIN`) debe rechazarse mediante una validación transaccional y bloqueante en el momento de la operación. Esta es la formalización, en el modelo de dominio, de la garantía de continuidad administrativa ya aprobada (`ADR-019`).

**Afecta:** `User`, `Role`, `UserRoleAssignment`
**Relacionado con:** `SPEC-AUTH-009`, `SPEC-AUTH-010`, REQ-AUTH-016, REQ-AUTH-033

---

## INV-AUTH-014 — `Role` y `Permission` no se eliminan físicamente
Un `Role` o un `Permission` nunca se elimina físicamente de la base de datos. Su ciclo de vida administrativo se expresa mediante el atributo `active` (`false` = desactivado, sin efecto en autorización). Esto preserva la integridad referencial de `UserRoleAssignment`, `AdGroupRoleMapping` y `SecurityAuditEvent` históricos.

**Afecta:** `Role`, `Permission`
**Relacionado con:** `SPEC-AUTH-010`, `ADR-020`

---

## INV-AUTH-015 — Una asignación explícita puede reemplazar (upgrade) una derivada, nunca duplicarla
Si un administrador otorga explícitamente a un `User` un `Role` que ya posee con `provenance = DERIVED_FROM_AD`, el `UserRoleAssignment` existente actualiza su `provenance` a `GRANTED_EXPLICITLY` en la misma fila; no se crea una segunda fila para el mismo par `(User, Role)` (consistente con la unicidad ya establecida para `UserRoleAssignment`). La operación inversa (una sincronización AD sobre una asignación `GRANTED_EXPLICITLY`) nunca ocurre, conforme a `INV-AUTH-005`.

**Afecta:** `UserRoleAssignment`
**Relacionado con:** `SPEC-AUTH-010`, REQ-AUTH-031
