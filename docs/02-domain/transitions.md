# Transiciones del Dominio — Módulo de Seguridad (AUTH)

---

## Transiciones de `User.status`

| Desde | Evento | Hacia | Condiciones |
|---|---|---|---|
| *(no existe)* | Primer login AD exitoso, sin ningún grupo mapeado | `PENDING_ONBOARDING` | Ver `SPEC-AUTH-001`, UC-AUTH-003. |
| *(no existe)* | Primer login AD exitoso, con al menos un grupo mapeado | `ACTIVE` | El rol proviene de un mapping ya aprobado administrativamente (INV-AUTH-004, INV-AUTH-005 no aplica aún porque es la primera asignación). |
| *(no existe)* | Alta administrativa de un `User` `LOCAL` | `ACTIVE` o `PENDING_ONBOARDING` según si se le asigna algún rol al crearlo | Decisión del administrador que lo crea. |
| `PENDING_ONBOARDING` | Un administrador asigna un rol explícito, o un login AD subsiguiente resuelve un mapping antes desconocido | `ACTIVE` | — |
| `ACTIVE` | Una sincronización AD deja al usuario sin ningún rol vigente (ni derivado ni explícito) | `PENDING_ONBOARDING` | Ver INV-AUTH-005. |
| `ACTIVE` | N intentos fallidos de autenticación consecutivos (solo aplica a verificación local; en AD, el propio AD gestiona su bloqueo — ver `SPEC-AUTH-001 §Seguridad`) | `LOCKED` | Umbral definido en `docs/03-architecture/security.md`. |
| `LOCKED` | Transcurre el cooldown, o un administrador desbloquea | `ACTIVE` (o el estado que corresponda según roles vigentes) | — |
| `ACTIVE` / `PENDING_ONBOARDING` / `LOCKED` | Un administrador deshabilita al usuario, o se detecta deshabilitado en AD en el próximo login | `DISABLED` | Revoca todos los `RefreshToken` vigentes del usuario (efecto colateral obligatorio, ver REQ-AUTH-011 aplicado automáticamente). |
| `DISABLED` | Un administrador reactiva al usuario, o se detecta reactivado en AD en su próximo login | `ACTIVE` o `PENDING_ONBOARDING` según roles vigentes al momento de la reactivación | Requiere reevaluación de roles, no reactivación ciega. Para usuarios AD, la reevaluación ocurre automáticamente vía UC-AUTH-004 (`SPEC-AUTH-001`) en el siguiente login. Para reactivación puramente administrativa (sin login AD de por medio), ver UC-AUTH-018 (`SPEC-AUTH-010`): el estado resultante se determina evaluando los roles vigentes del usuario en ese momento (derivados + explícitos), igual que en cualquier otra transición de este dominio. |
| Cualquiera excepto `DEPROVISIONED` | Retiro formal del usuario (administrativo) | `DEPROVISIONED` | Estado terminal; no se han definido transiciones de salida. Rechazada mediante INV-AUTH-013 si el usuario es el único `User` `ACTIVE` con el rol `MASTER_ADMIN` (ver `SPEC-AUTH-010`). |

**Nota:** las transiciones hacia `DISABLED` y `LOCKED` (cuando son administrativas, no automáticas por intentos fallidos) también quedan sujetas a INV-AUTH-013: no pueden ejecutarse sobre el único `User` `ACTIVE` con el rol `MASTER_ADMIN`. Ver `SPEC-AUTH-010 §5` (RN-08).

---

## Transiciones de `RefreshToken`

| Desde | Evento | Hacia | Condiciones |
|---|---|---|---|
| *(no existe)* | Login exitoso o rotación exitosa | Vigente | Se crea con `expiresAt` según configuración (hipótesis: 7 días). |
| Vigente | Uso exitoso para refresh | Revocado (por rotación) + creación atómica del hijo | INV-AUTH-006. |
| Vigente | Logout individual | Revocado (sin reemplazo) | Solo afecta la rama activa de ese dispositivo/sesión. |
| Vigente (cualquiera de la familia) | Logout-all, o revocación administrativa, o `User` pasa a `DISABLED`/`DEPROVISIONED` | Revocado (toda la familia) | — |
| Revocado (por rotación) | Se presenta nuevamente ese mismo token | Toda la familia pasa a Revocado (si no lo estaba ya) | INV-AUTH-007; se registra `SecurityAuditEvent` de severidad alta. |
| Vigente | Transcurre `expiresAt` | Expirado | No requiere acción activa; se trata como inválido en el próximo uso. |

---

## Transiciones de `Role.active` / `Permission.active`

| Desde | Evento | Hacia | Condiciones |
|---|---|---|---|
| *(creación)* | Alta administrativa | `active = true` | Ver `SPEC-AUTH-010`. |
| `active = true` | Un administrador desactiva | `active = false` | Rechazada si `isSystemRole`/`isSystemPermission = true` (INV-AUTH-011, INV-AUTH-014), o si el `Role` es `MASTER_ADMIN` y su desactivación dejaría al sistema sin ningún `User` `ACTIVE` con ese rol (INV-AUTH-013). |
| `active = false` | Un administrador reactiva | `active = true` | Sin restricciones adicionales. |

No existe transición hacia un estado de eliminación física (INV-AUTH-014).
