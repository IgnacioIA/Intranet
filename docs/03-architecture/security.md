# Seguridad — Módulo de Seguridad (AUTH)

## 1. Autenticación

Dos providers: `LOCAL` (contraseña verificada por la aplicación) y `ACTIVE_DIRECTORY` (contraseña verificada por AD; la aplicación nunca la almacena). Ver `SPEC-AUTH-001`.

## 2. Autorización

RBAC + Permissions, semántica aditiva estricta (permiso presente → permitido; ausente → denegado; sin reglas DENY explícitas). Fuente de verdad: base de datos, evaluada en cada request protegido — nunca el contenido cacheado del JWT (REQ-AUTH-015, INV-AUTH-012). Ver `SPEC-AUTH-006`.

## 3. Roles y permisos — responsabilidad AD vs. aplicación

| Responsabilidad | Autoridad |
|---|---|
| Verificación de contraseña de una cuenta AD | Active Directory |
| Política de contraseña, bloqueo por intentos fallidos, expiración de cuenta **AD** | Active Directory (no duplicado por la aplicación) |
| Verificación de contraseña de una cuenta LOCAL | Aplicación (Argon2id) |
| Política de contraseña, bloqueo por intentos fallidos, expiración de cuenta **LOCAL** | Aplicación |
| Qué puede hacer un usuario dentro de la aplicación (roles/permisos) | Aplicación, siempre — incluso para usuarios AD, mediante el mapping explícito |

## 4. Protección de datos

- Contraseñas `LOCAL`: Argon2id (`ADR-011`), nunca reversibles.
- Refresh Tokens y tokens de recuperación de contraseña: almacenados hasheados (REQ-AUTH-020).
- Ningún dato sensible en logs ni auditoría (INV-AUTH-009, REQ-AUTH-023).

## 5. Catálogo de eventos de Security Audit

| Evento | Disparado por |
|---|---|
| `LOGIN_ATTEMPT` | Inicio de un intento de autenticación (antes de resolver éxito/fallo). |
| `LOGIN_SUCCESS` | Autenticación exitosa (LOCAL o AD). |
| `LOGIN_FAILURE` | Credenciales inválidas, usuario inexistente, cuenta `LOCKED`/`DISABLED`. |
| `LOGOUT` | Cierre de sesión individual. |
| `LOGOUT_ALL` | Cierre de todas las sesiones (autoservicio). |
| `ADMIN_SESSION_REVOCATION` | Revocación forzada de sesiones por un administrador. |
| `AUTHORIZATION_GRANTED` | Autorización concedida sobre un recurso protegido (uso selectivo — ver nota). |
| `AUTHORIZATION_DENIED` | Autorización denegada. |
| `TOKEN_CREATED` | Emisión de Access/Refresh Token tras login. |
| `TOKEN_REFRESHED` | Rotación exitosa de Refresh Token. |
| `TOKEN_REUSE_DETECTED` | Reutilización de un Refresh Token ya revocado (severidad alta). |
| `TOKEN_REVOKED` | Revocación de un Refresh Token (logout, logout-all o administrativa). |
| `ACCOUNT_LOCKED` | Transición de `User.status` a `LOCKED`. |
| `ACCOUNT_DISABLED` | Transición de `User.status` a `DISABLED`. |
| `PASSWORD_CHANGED` | Cambio de contraseña autenticado. |
| `PASSWORD_RECOVERY_REQUESTED` | Solicitud de recuperación (auditar sin registrar si la cuenta existe). |
| `PASSWORD_RECOVERY_CONFIRMED` | Confirmación exitosa de recuperación. |
| `AD_LOOKUP_FAILURE` | Fallo puntual al consultar identidad/grupos en AD. |
| `AD_CONNECTION_FAILURE` | AD inalcanzable. |
| `AD_GROUP_UNMAPPED` | Un grupo AD del usuario no tiene mapping conocido (informativo, no error). |
| `AD_USER_PROVISIONED` | Alta de la Shadow Identity de un usuario AD nuevo. |
| `AD_MAPPING_CHANGED` | Alta/baja/modificación de un `AdGroupRoleMapping`. |
| `RATE_LIMIT_EXCEEDED` | Límite de intentos de autenticación excedido. |
| `MASTER_ADMIN_BOOTSTRAPPED` | Bootstrap inicial de la cuenta administrativa local. |
| `USER_CREATED` | Alta administrativa de un usuario `LOCAL` (`SPEC-AUTH-010`). |
| `USER_UPDATED` | Actualización de identidad de un usuario `LOCAL`. |
| `USER_ENABLED` | Reactivación administrativa de un usuario. |
| `USER_DISABLED` | Deshabilitación administrativa de un usuario (distinto de `ACCOUNT_DISABLED`, que registra la transición de dominio; este evento registra la acción administrativa que la originó). |
| `USER_LOCKED` | Bloqueo administrativo manual de un usuario (distinto del bloqueo automático por intentos fallidos). |
| `USER_UNLOCKED` | Desbloqueo administrativo de un usuario. |
| `USER_DEPROVISIONED` | Retiro formal (terminal) de un usuario. |
| `ROLE_CREATED` | Alta de un `Role`. |
| `ROLE_UPDATED` | Modificación de descripción, permisos o reactivación de un `Role`. |
| `ROLE_DISABLED` | Desactivación de un `Role` (`active = false`). |
| `PERMISSION_CREATED` | Alta de una `Permission`. |
| `PERMISSION_UPDATED` | Modificación de descripción o reactivación de una `Permission`. |
| `PERMISSION_DISABLED` | Desactivación de una `Permission` (`active = false`). |
| `ROLE_ASSIGNED` | Asignación explícita de un `Role` a un usuario (incluye el caso de *upgrade* desde `DERIVED_FROM_AD`). |
| `ROLE_REVOKED` | Revocación de una asignación `GRANTED_EXPLICITLY`. |
| `ADMIN_OPERATION_DENIED` | Rechazo de una operación administrativa por una regla de negocio (ej. violación de `INV-AUTH-013`, transición de estado inválida) — distinto de `AUTHORIZATION_DENIED`, que es específicamente por falta de permiso. |

**Política sobre `AUTHORIZATION_GRANTED`** (Decision Ledger, 2026-09-05): auditar cada autorización concedida generaría volumen alto sin valor proporcional para el tráfico general. `AUTHORIZATION_GRANTED` se audita únicamente para acciones administrativas sensibles (revocación administrativa de sesiones — `SPEC-AUTH-004`; gestión del mapping AD — `SPEC-AUTH-008`); no se audita para el tráfico general de lectura/escritura ordinario. `AUTHORIZATION_DENIED`, en cambio, se audita siempre, sin excepción, en cualquier endpoint protegido.

## 6. Rate limiting

Por combinación IP + identidad (REQ-AUTH-018) — se evalúan ambas dimensiones de forma independiente; basta con que una se exceda para rechazar. Estado en base de datos en V1 (no en memoria de proceso — REQ-AUTH-022), con un `RateLimiterPort` preparado para un adapter distribuido (Redis) sin implementarlo (`ADR-013`).

**Umbrales concretos** (Fase 4 de implementación — parámetros de bajo impacto y reversibles, `.claude/core/decision-authority.md`, no requieren ADR):

| Dimensión | Límite | Ventana |
|---|---|---|
| Login por IP | 20 intentos | 15 minutos |
| Login por identidad (`provider`+`username`) | 5 intentos | 15 minutos |
| Bloqueo automático de cuenta `LOCAL` (`SPEC-AUTH-001` RN-07, `02-domain/transitions.md`) | 5 intentos fallidos consecutivos | Cooldown de 15 minutos (`LOCKED` con `lockedUntil`) |

El bloqueo automático de cuenta es distinto del rate limiting por IP/identidad: el primero es un contador propio de cada `User` (`failedLoginAttempts`); el segundo es un límite agregado independiente del resultado (correcto o incorrecto) de cada intento.

## 7. Amenazas relevantes consideradas

- Fuerza bruta / credential stuffing contra login → mitigado por rate limiting + lockout.
- Robo de Refresh Token → mitigado por rotación + detección de reuse (`ADR-008`).
- XSS exfiltrando tokens → mitigado por transporte (Access Token en memoria, Refresh Token en cookie `httpOnly`) — `ADR-010`.
- Enumeración de usuarios vía login/recovery → mitigado por respuestas equivalentes (REQ-AUTH-025).
- Escalado de privilegios vía grupos AD no controlados → mitigado por mapping explícito y auditado (`ADR-005`).
- Compromiso de la única cuenta administrativa local → mitigado por bootstrap sin secreto embebido y cooldown no permanente (ver `SPEC-AUTH-009`).

## 8. Controles adoptados — resumen

Ver ADRs `ADR-001` a `ADR-020` para el detalle de cada decisión y sus alternativas consideradas.
