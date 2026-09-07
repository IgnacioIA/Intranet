# SPEC-AUTH-002 — Renovación de sesión (Refresh Token)

**Estado:** APPROVED
**Versión:** 1.1

## 1. Objetivo
Permitir obtener un nuevo Access Token sin repetir credenciales, mediante un Refresh Token válido, con rotación y detección de reutilización.

## 2. Alcance
### Incluye
Canje de Refresh Token vigente por un nuevo par de tokens; revocación de la familia completa ante reutilización detectada.
### No incluye
Login inicial (`SPEC-AUTH-001`), logout (`SPEC-AUTH-003`).

## 3. Actores
Usuario autenticado (a través de su cliente/frontend, sin intervención manual).

## 4. Requisitos relacionados
REQ-AUTH-009, REQ-AUTH-020

## 5. Reglas de negocio
- RN-01: El canje de un Refresh Token vigente revoca ese token y emite uno nuevo de la misma familia, de forma atómica (INV-AUTH-006).
- RN-02: El refresh **no** consulta Active Directory ni recalcula roles derivados de AD; utiliza el estado ya persistido (esa recomputación ocurre únicamente en el login, `SPEC-AUTH-001` UC-AUTH-004).
- RN-03: El refresh sí verifica que `User.status = ACTIVE` en el momento del canje; si no, se deniega y se revoca la familia (una cuenta deshabilitada no debe poder seguir renovando sesión).
- RN-04: Presentar un Refresh Token ya revocado revoca inmediatamente toda su familia (INV-AUTH-007).
- RN-05: No existe grace period: la rotación es estricta (`ADR-008`). Un segundo uso del mismo token, aunque sea concurrente y legítimo (dos pestañas, un reintento de red), se trata igual que cualquier reutilización (RN-04): revoca toda la familia y exige nuevo login. Es responsabilidad del frontend serializar ("single-flight") sus llamadas de refresh para evitar este caso.

## 6. Casos de uso

### UC-AUTH-005 — Renovar sesión

**Actor:** Usuario autenticado (cliente).

**Precondiciones:** El cliente posee un Refresh Token (cookie) potencialmente vigente.

**Flujo principal:**
1. El cliente envía la solicitud de refresh (el Refresh Token viaja en la cookie, no en el cuerpo).
2. El sistema localiza el `RefreshToken` por su hash.
3. El sistema verifica: no expirado, no revocado, `User.status = ACTIVE`.
4. El sistema revoca el token presentado y emite uno nuevo de la misma familia (atómico, RN-01).
5. El sistema emite un nuevo Access Token.
6. El sistema registra `TOKEN_REFRESHED`.

**Flujos alternativos:**
- 3a. Token expirado → 401, no se revoca nada adicional (ya está fuera de uso por expiración natural).
- 3b. Token ya revocado (reutilización) → revoca toda la familia; registra `TOKEN_REUSE_DETECTED` (severidad alta); 401.
- 3c. `User.status != ACTIVE` → deniega; revoca la familia (RN-03); registra `LOGIN_FAILURE` o evento equivalente de denegación.

**Errores:** refresh token inválido/expirado/revocado, cuenta no activa.

**Resultado esperado:** nuevo par de tokens válido, o rechazo con revocación de la familia cuando corresponda a una señal de compromiso.

## 7. Casos límite
- Dos requests de refresh concurrentes con el mismo token (dos pestañas, retry de red) → **resuelto: sin grace period** (RN-05, `ADR-008`). Solo el primero en completar la operación atómica (INV-AUTH-006) prospera; el segundo encuentra el token ya revocado y activa la detección de reuse estándar (RN-04), revocando toda la familia. Es un resultado intencional de la estrategia *strict rotation*.

## 8. Criterios de aceptación

```gherkin
Feature: Renovación de sesión

  Scenario: Refresh exitoso
    Given un Refresh Token vigente de un usuario ACTIVE
    When el cliente solicita renovar la sesión
    Then el sistema emite un nuevo Access Token
    And el Refresh Token anterior queda revocado
    And se emite un nuevo Refresh Token de la misma familia

  Scenario: Refresh Token expirado
    Given un Refresh Token cuya fecha de expiración ya pasó
    When el cliente intenta renovar la sesión
    Then el sistema responde 401 sin revocar la familia

  Scenario: Reutilización de Refresh Token ya revocado
    Given un Refresh Token que ya fue utilizado y revocado por rotación
    When se presenta nuevamente ese mismo token
    Then toda su familia queda revocada
    And se registra un evento TOKEN_REUSE_DETECTED de severidad alta
    And el cliente debe iniciar sesión nuevamente

  Scenario: Refresh concurrente sin grace period
    Given un Refresh Token vigente
    When dos solicitudes de renovación llegan concurrentemente con ese mismo token
    Then solo una de ellas prospera y rota el token
    And la otra encuentra el token ya revocado
    And se activa la detección de reuse, revocando toda la familia

  Scenario: Refresh con cuenta deshabilitada
    Given un usuario cuyo status pasó a DISABLED después de emitido su Refresh Token
    When intenta renovar la sesión
    Then el sistema deniega la renovación
    And revoca la familia de ese Refresh Token
```

## 9. Dependencias
`TokenPort`, `UserRepositoryPort`, `AuditPort`. Depende de que exista una sesión previa emitida por `SPEC-AUTH-001`.

## 10. Restricciones
El refresh no debe depender de la disponibilidad de `IdentityDirectoryPort` (RN-02) — debe funcionar aunque AD esté caído, siempre que la sesión ya haya sido establecida.

## 11. Seguridad
Ver ADR-008 (rotation) y ADR-010 (transporte).

## 12. API Contract

### Método
`POST`

### Endpoint
`/auth/refresh`

### Autenticación
Refresh Token vía cookie `HttpOnly` con `Path=/auth` (corregido de `/auth/refresh` en el Decision Ledger del 2026-09-06, Fase 22 — ver ADR-010). No requiere Access Token.

### Autorización
No aplica (la validez del propio Refresh Token es la autorización).

### Headers
No requiere headers de autorización adicionales.

### Request
Sin cuerpo (el token viaja en la cookie).

### Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresInSeconds": 900,
  "tokenType": "Bearer"
}
```
Se emite un nuevo `Set-Cookie` con el Refresh Token rotado.

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200 | OK | Renovación exitosa. |
| 401 | Unauthorized | Token ausente, expirado, revocado/reutilizado, o cuenta no `ACTIVE`. |

### Errores (RFC 7807)
| Código | Error (`type`) | Condición |
|---|---|---|
| 401 | `invalid-refresh-token` | Token ausente/expirado/inválido. |
| 401 | `refresh-token-reused` | Reutilización detectada (mensaje genérico hacia el cliente; el detalle queda solo en auditoría). |

### Restricciones del contrato
No debe revelar en la respuesta si la causa fue "reutilización" vs "expiración" — el cliente en ambos casos debe simplemente redirigir a login. La distinción es solo interna/de auditoría.

## 13. Referencias de dominio
- Entidad: RefreshToken, RefreshTokenFamily
- Invariante: INV-AUTH-006, INV-AUTH-007
- Estado: ver `02-domain/states.md` (estados de RefreshToken)

## 14. Trazabilidad
- Requisitos: REQ-AUTH-009, REQ-AUTH-020
- Dominio: RefreshToken
- ADR: ADR-007, ADR-008, ADR-010

## 15. GAPs
Sin GAPs abiertos. GAP-AUTH-003 (grace period ante refresh concurrente) fue resuelto por el Decision Ledger del 2026-09-05: sin grace period, strict rotation.
