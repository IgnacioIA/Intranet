# Estados del Dominio — Módulo de Seguridad (AUTH)

Aclaración: estos son estados de **conceptos del dominio** (`User`, `RefreshToken`), no estados de una `SPEC` (que siguen `.claude/sdd/lifecycle.md`) ni estados de infraestructura.

---

## Estados de `User`

| Estado | Significado |
|---|---|
| `PENDING_ONBOARDING` | El usuario no posee ningún rol válido más allá de `ONBOARDING_USER`. Solo puede acceder a la información de onboarding. |
| `ACTIVE` | El usuario posee al menos un rol válido y puede operar normalmente según sus permisos. |
| `LOCKED` | El usuario está temporalmente impedido de autenticarse por exceso de intentos fallidos. Se origina automáticamente y se revierte por cooldown o intervención administrativa. |
| `DISABLED` | El usuario fue deshabilitado explícitamente (por un administrador, o detectado deshabilitado en Active Directory). No puede autenticarse ni operar. |
| `DEPROVISIONED` | El usuario fue retirado formalmente. Se conserva por trazabilidad/auditoría histórica, pero no puede autenticarse ni operar, y no participa de ningún flujo de reactivación automática. |

**Regla transversal (INV-AUTH-012 y REQ-AUTH-015):** ningún estado distinto de `ACTIVE` permite acceso, incluso si el `Access Token` presentado sigue siendo válido criptográficamente y no ha expirado.

---

## Estados de `RefreshToken`

| Estado (derivado de atributos, no de un campo `status` propio) | Condición |
|---|---|
| Vigente | `revokedAt = null` y `expiresAt` en el futuro. |
| Expirado | `revokedAt = null` y `expiresAt` en el pasado. |
| Revocado por rotación | `revokedAt != null` y `replacedByTokenId != null`. |
| Revocado por logout/logout-all/administración | `revokedAt != null` y `replacedByTokenId = null`. |
| Revocado por reutilización detectada (familia completa) | `revokedAt != null`, marcado junto con el resto de su `RefreshTokenFamily` en la misma operación. |

No se modela como una máquina de estados con un campo enumerado propio: se deriva de `revokedAt`, `expiresAt` y `replacedByTokenId`, evitando duplicar información que ya existe en esos atributos (regla de no duplicación, `.claude/documentation/structure.md §12`).

---

## Estados de `Role` y `Permission`

No constituyen una máquina de estados con transiciones condicionadas: es un atributo booleano `active` (`true`/`false`), sin estados intermedios ni ciclo de vida propio más allá de eso. Ver `ADR-020` y `SPEC-AUTH-010`. Un `Role`/`Permission` con el flag de sistema correspondiente (`isSystemRole`/`isSystemPermission`) permanece siempre `active = true`.
