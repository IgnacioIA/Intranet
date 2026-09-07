# SPEC-AUTH-001 — Autenticación de usuarios (LOCAL y Active Directory)

**Estado:** APPROVED
**Versión:** 1.1

## 1. Objetivo
Permitir que un usuario, cuya identidad es gestionada localmente o por Active Directory, obtenga una sesión válida (Access Token + Refresh Token) mediante credenciales, y que un usuario de Active Directory que se autentica por primera vez quede correctamente aprovisionado con el mínimo privilegio necesario.

## 2. Alcance

### Incluye
- Verificación de credenciales `LOCAL` y `ACTIVE_DIRECTORY`.
- Aprovisionamiento de la Shadow Identity de un usuario AD nuevo (UC-AUTH-003).
- Sincronización de roles derivados de AD en cada login (UC-AUTH-004).
- Emisión de tokens tras autenticación exitosa.
- Limitación de tasa de intentos (rate limiting).

### No incluye
- Renovación de sesión (`SPEC-AUTH-002`), cierre de sesión (`SPEC-AUTH-003`), gestión de contraseña (`SPEC-AUTH-007`), administración del mapping AD (`SPEC-AUTH-008`).

## 3. Actores
- Usuario `LOCAL`.
- Usuario `ACTIVE_DIRECTORY`.
- Active Directory (sistema externo, autoridad de credenciales AD).

## 4. Requisitos relacionados
- REQ-AUTH-001, REQ-AUTH-002, REQ-AUTH-003, REQ-AUTH-004, REQ-AUTH-005, REQ-AUTH-007, REQ-AUTH-008, REQ-AUTH-018, REQ-AUTH-025

## 5. Reglas de negocio
- RN-01: Un usuario con `status` distinto de `ACTIVE` o `PENDING_ONBOARDING` (es decir, `LOCKED` o `DISABLED`) no puede autenticarse, independientemente de si sus credenciales son correctas.
- RN-02: La verificación de contraseña `LOCAL` es responsabilidad exclusiva de la aplicación; la verificación de contraseña AD es responsabilidad exclusiva de Active Directory.
- RN-03: Un grupo AD sin entrada en `AdGroupRoleMapping` no otorga ningún rol (INV-AUTH-004, INV-AUTH-010).
- RN-04: Todo `UserRoleAssignment` con `provenance = DERIVED_FROM_AD` se recalcula en cada login AD exitoso; los de `provenance = GRANTED_EXPLICITLY` nunca se modifican por este proceso (INV-AUTH-005).
- RN-05: Si, tras evaluar todos los grupos AD del usuario, no resulta ningún rol vigente, el usuario queda/permanece en `PENDING_ONBOARDING` con únicamente el rol `ONBOARDING_USER`.
- RN-06: Ningún mensaje de error de login debe permitir distinguir "usuario inexistente" de "credencial incorrecta" (REQ-AUTH-025).
- RN-07: Los intentos fallidos de autenticación `LOCAL` incrementan un contador que, superado un umbral, transiciona la cuenta a `LOCKED` (ver `02-domain/transitions.md`). AD gestiona su propio bloqueo; la aplicación no lo duplica (`docs/03-architecture/security.md §3`).
- RN-08: El `provider` (`LOCAL` o `ACTIVE_DIRECTORY`) se recibe explícitamente en la solicitud de login. El sistema no autodetecta el proveedor a partir del `username` (`ADR-018`).
- RN-09: Si las credenciales AD son correctas pero falla la obtención o evaluación de los grupos del usuario, el sistema falla cerrado: no se emiten tokens (`ADR-017`).

## 6. Casos de uso

### UC-AUTH-001 — Iniciar sesión (LOCAL)

**Actor:** Usuario `LOCAL`.

**Precondiciones:** El usuario posee una cuenta `LOCAL` con `status` no `LOCKED` ni `DISABLED`.

**Flujo principal:**
1. El usuario envía `provider`, `username` y `password`.
2. El sistema localiza el `User(provider=LOCAL, username=...)`.
3. El sistema verifica que `status` permita autenticación (RN-01).
4. El sistema verifica la contraseña contra `PasswordCredential` mediante el Port de hashing.
5. El sistema resetea el contador de intentos fallidos.
6. El sistema emite Access Token + Refresh Token.
7. El sistema registra `LOGIN_SUCCESS`.

**Flujos alternativos:**
- 4a. Contraseña incorrecta → incrementa contador de intentos fallidos; si supera el umbral, transiciona a `LOCKED`; registra `LOGIN_FAILURE`; responde error genérico (RN-06).
- 2a. Usuario inexistente → responde el mismo error genérico que 4a, sin distinguir la causa (RN-06); registra `LOGIN_FAILURE`.
- 3a. `status = LOCKED` o `DISABLED` → responde error genérico equivalente a credencial incorrecta (no revelar el estado específico); registra `LOGIN_FAILURE`.

**Errores:** credenciales inválidas (genérico), límite de intentos excedido (`RATE_LIMIT_EXCEEDED`).

**Resultado esperado:** sesión válida (Access + Refresh Token) o rechazo sin información que permita enumerar cuentas.

---

### UC-AUTH-002 — Iniciar sesión (Active Directory)

**Actor:** Usuario `ACTIVE_DIRECTORY`.

**Precondiciones:** Active Directory es alcanzable; el usuario posee cuenta AD válida.

**Flujo principal:**
1. El usuario envía `provider`, `username` y `password`.
2. El sistema delega la verificación de credenciales a `IdentityDirectoryPort` (bind AD).
3. AD acepta las credenciales y el sistema obtiene `objectGUID` y grupos del usuario.
4. El sistema localiza `User(provider=ACTIVE_DIRECTORY, externalId=objectGUID)`.
5. Si no existe → dispara UC-AUTH-003 (Aprovisionar usuario AD nuevo).
6. Si existe → dispara UC-AUTH-004 (Sincronizar roles derivados de AD) y sincroniza `username`/atributos mutables.
7. El sistema verifica que `status` resultante permita autenticación (RN-01).
8. El sistema emite Access Token + Refresh Token.
9. El sistema registra `LOGIN_SUCCESS`.

**Flujos alternativos:**
- 2a. AD rechaza credenciales → responde error genérico equivalente a UC-AUTH-001 (RN-06); registra `LOGIN_FAILURE`.
- 2b. AD inalcanzable → responde una condición **distinguible** de credencial incorrecta (ej. "servicio de autenticación no disponible"); registra `AD_CONNECTION_FAILURE`. Esta es una excepción intencional a RN-06: la disponibilidad del servicio no es información de la cuenta.
- 3a. AD acepta las credenciales pero falla la obtención/evaluación de los grupos del usuario → **fail closed** (RN-09): no se emiten tokens; responde una condición de servicio (503), distinguible de credencial incorrecta; registra `AD_LOOKUP_FAILURE` y el log técnico correspondiente. No se trata como "credenciales inválidas" ni se procede con roles previamente persistidos.
- 7a. `status = DISABLED` tras sincronización (ver UC-AUTH-004, caso "usuario deshabilitado en AD detectado") → responde error genérico; registra `LOGIN_FAILURE`.

**Errores:** credenciales inválidas (genérico), AD no disponible, límite de intentos excedido.

**Resultado esperado:** sesión válida, con roles correctamente sincronizados, o rechazo/condición distinguible según corresponda.

---

### UC-AUTH-003 — Aprovisionar usuario AD nuevo

**Actor:** Sistema (disparado dentro de UC-AUTH-002).

**Precondiciones:** No existe `User(provider=ACTIVE_DIRECTORY, externalId=objectGUID)`.

**Flujo principal:**
1. El sistema crea la Shadow Identity (`User`) con los atributos mínimos necesarios (username, displayName, email, `objectGUID`).
2. El sistema evalúa cada grupo AD del usuario contra `AdGroupRoleMapping`.
3. Los grupos con mapping otorgan el rol correspondiente (`provenance = DERIVED_FROM_AD`).
4. Los grupos sin mapping se registran como `AD_GROUP_UNMAPPED` (informativo), sin efecto de acceso.
5. Si el paso 3 otorgó al menos un rol → `status = ACTIVE`.
6. Si el paso 3 no otorgó ningún rol → `status = PENDING_ONBOARDING`, con el rol de sistema `ONBOARDING_USER`.
7. El sistema registra `AD_USER_PROVISIONED` con el detalle de grupos evaluados y resultado.

**Flujos alternativos:** ninguno adicional relevante.

**Errores:** ninguno propio (los errores de AD ya se manejan en UC-AUTH-002).

**Resultado esperado:** Shadow Identity creada, con `status` y roles determinados exclusivamente por el resultado del mapping — nunca por una noción difusa de "acceso de lectura general" (RN-05, INV-AUTH-004).

---

### UC-AUTH-004 — Sincronizar roles derivados de Active Directory

**Actor:** Sistema (disparado dentro de UC-AUTH-002, en cada login AD de un usuario ya existente).

**Precondiciones:** Existe `User(provider=ACTIVE_DIRECTORY, externalId=objectGUID)`.

**Flujo principal:**
1. El sistema obtiene los grupos AD vigentes del usuario.
2. El sistema evalúa cada grupo contra `AdGroupRoleMapping` vigente.
3. El sistema elimina todo `UserRoleAssignment(provenance=DERIVED_FROM_AD)` que ya no corresponda a un grupo vigente mapeado (INV-AUTH-005).
4. El sistema agrega todo `UserRoleAssignment(provenance=DERIVED_FROM_AD)` nuevo que corresponda a un grupo recién mapeado.
5. Los `UserRoleAssignment(provenance=GRANTED_EXPLICITLY)` permanecen sin cambios.
6. Si, tras la sincronización, el usuario no posee ningún rol vigente (ni derivado ni explícito) → `status = PENDING_ONBOARDING`.
7. Si el usuario posee al menos un rol vigente y su `status` anterior era `PENDING_ONBOARDING` → `status = ACTIVE`.
8. Grupos sin mapping conocido → `AD_GROUP_UNMAPPED` (informativo).

**Flujos alternativos:**
- Usuario detectado `DISABLED`/inexistente en AD durante esta consulta → el sistema transiciona `User.status = DISABLED` y revoca todos sus Refresh Tokens vigentes (efecto colateral obligatorio, ver `02-domain/transitions.md`).
- Fallo al obtener/evaluar los grupos AD (`AD_LOOKUP_FAILURE`) → **fail closed** (RN-09, `ADR-017`): la sincronización se aborta y el login completo (UC-AUTH-002) falla con una condición de servicio (503). No se conserva ni se asume ningún rol previamente persistido para completar este intento de login.

**Errores:** `AD_LOOKUP_FAILURE` (fail closed — ver RN-09).

**Resultado esperado:** el conjunto de roles derivados refleja exactamente los grupos AD vigentes del usuario en el momento del login.

## 7. Casos límite
- Un usuario tiene grupos AD mapeados y no mapeados simultáneamente → solo los mapeados producen rol; los no mapeados se auditan (RN-03).
- Un usuario es eliminado y recreado en AD → nuevo `objectGUID`, tratado como identidad nueva (comportamiento intencional, ver ADR-004).
- Fallo de `AD_LOOKUP_FAILURE` durante sincronización (credenciales AD correctas, pero falla la obtención/evaluación de grupos) → **resuelto: fail closed** (RN-09, `ADR-017`). No se emiten tokens; se responde una condición de servicio (503); el usuario puede reintentar cuando el servicio de directorio se recupere.
- Reactivación de un usuario `DISABLED` (por AD o por administrador) → no reactiva ciegamente a `ACTIVE`; debe reevaluar roles vigentes (ver `02-domain/transitions.md`).

## 8. Criterios de aceptación

```gherkin
Feature: Autenticación de usuarios

  Scenario: Login LOCAL exitoso
    Given un usuario LOCAL activo con contraseña conocida
    When el usuario inicia sesión con esa contraseña
    Then el sistema emite un Access Token y un Refresh Token
    And se registra un evento LOGIN_SUCCESS

  Scenario: Login LOCAL con contraseña incorrecta
    Given un usuario LOCAL activo
    When el usuario inicia sesión con una contraseña incorrecta
    Then el sistema responde un error genérico de credenciales inválidas
    And se registra un evento LOGIN_FAILURE

  Scenario: Login con usuario inexistente no revela esa condición
    Given que no existe ningún usuario con el username indicado
    When se intenta iniciar sesión con ese username
    Then la respuesta es indistinguible de una contraseña incorrecta

  Scenario: Login AD exitoso de usuario ya existente
    Given un usuario ACTIVE_DIRECTORY previamente aprovisionado y activo
    When se autentica correctamente contra AD
    Then sus roles derivados de AD se recalculan según sus grupos vigentes
    And el sistema emite un Access Token y un Refresh Token

  Scenario: Usuario AD nuevo sin ningún grupo mapeado
    Given un usuario AD que nunca se autenticó en la aplicación
    And ninguno de sus grupos AD tiene mapping conocido
    When se autentica correctamente contra AD por primera vez
    Then se crea su Shadow Identity con status PENDING_ONBOARDING
    And se le asigna únicamente el rol ONBOARDING_USER
    And cada grupo no mapeado genera un evento AD_GROUP_UNMAPPED

  Scenario: Usuario AD nuevo con al menos un grupo mapeado
    Given un usuario AD que nunca se autenticó en la aplicación
    And al menos uno de sus grupos AD tiene mapping a un rol
    When se autentica correctamente contra AD por primera vez
    Then se crea su Shadow Identity con status ACTIVE
    And se le asignan los roles correspondientes a sus grupos mapeados

  Scenario: Un rol asignado explícitamente sobrevive a la sincronización AD
    Given un usuario ACTIVE_DIRECTORY con un rol GRANTED_EXPLICITLY
    And ese rol no corresponde a ningún grupo AD del usuario
    When el usuario inicia sesión nuevamente contra AD
    Then el rol GRANTED_EXPLICITLY permanece asignado sin cambios

  Scenario: Active Directory inalcanzable
    Given que Active Directory no responde
    When un usuario AD intenta iniciar sesión
    Then el sistema responde una condición distinguible de credencial incorrecta
    And se registra un evento AD_CONNECTION_FAILURE

  Scenario: Fallo cerrado ante error de sincronización de grupos AD
    Given un usuario AD cuyas credenciales son correctas
    And falla la obtención o evaluación de sus grupos AD
    When intenta iniciar sesión
    Then el sistema no emite ningún token
    And responde una condición de servicio distinguible de credencial incorrecta
    And se registra un evento AD_LOOKUP_FAILURE

  Scenario: Cuenta bloqueada por intentos fallidos
    Given un usuario LOCAL cuyo status es LOCKED
    When intenta iniciar sesión con la contraseña correcta
    Then el sistema rechaza el intento con el mismo error genérico de credenciales inválidas
```

## 9. Dependencias
- `IdentityDirectoryPort`, `UserRepositoryPort`, `TokenPort`, `PasswordHasherPort`, `AuditPort`, `RateLimiterPort`.
- `SPEC-AUTH-008` (el mapping debe existir para que UC-AUTH-003/004 tengan efecto).

## 10. Restricciones
- El dominio no debe depender de una implementación concreta de directorio (ADR-002).

## 11. Seguridad
- Rate limiting IP + identidad (REQ-AUTH-018).
- No enumeración de usuarios (RN-06, REQ-AUTH-025).
- AD gestiona su propia política de bloqueo/expiración; la aplicación no la duplica (docs/03-architecture/security.md §3).

## 12. API Contract

### Método
`POST`

### Endpoint
`/auth/login`

### Autenticación
No requiere autenticación previa (es el propio endpoint de autenticación).

### Autorización
No aplica.

### Headers
| Header | Obligatorio | Descripción |
|---|---|---|
| `Content-Type` | Sí | `application/json` |

### Parámetros de ruta
No aplica.

### Parámetros de consulta
No aplica.

### Request
```json
{
  "provider": "LOCAL",
  "username": "jdoe",
  "password": "string"
}
```
`provider` es obligatorio (`LOCAL` o `ACTIVE_DIRECTORY`).

### Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresInSeconds": 900,
  "tokenType": "Bearer"
}
```
El Refresh Token se entrega mediante cookie `Set-Cookie` (`HttpOnly`, `Secure`, `SameSite`, `Path=/auth`), no en el cuerpo (ver ADR-010; `Path` corregido de `/auth/refresh` a `/auth` en el Decision Ledger del 2026-09-06, Fase 22 — el valor original impedía que `/auth/logout` y `/auth/logout/all` recibieran la cookie en un navegador real).

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200 | OK | Autenticación exitosa. |
| 400 | Bad Request | `provider` ausente o con valor inválido. |
| 401 | Unauthorized | Credenciales inválidas, usuario inexistente, cuenta `LOCKED`/`DISABLED` (mensaje genérico único). |
| 429 | Too Many Requests | Límite de intentos excedido. |
| 503 | Service Unavailable | Active Directory inalcanzable, o falla la obtención de grupos AD tras autenticación exitosa (fail closed, RN-09) — solo para `provider = ACTIVE_DIRECTORY`. |

### Errores (RFC 7807)
| Código | Error (`type`) | Condición |
|---|---|---|
| 400 | `invalid-provider` | `provider` ausente o con valor distinto de `LOCAL`/`ACTIVE_DIRECTORY`. |
| 401 | `invalid-credentials` | Ver arriba, mensaje único sin distinguir causa. |
| 429 | `rate-limit-exceeded` | Ver REQ-AUTH-018. |
| 503 | `ad-unavailable` | AD inalcanzable. |
| 503 | `ad-sync-failed` | Credenciales AD correctas, pero falla la obtención/evaluación de grupos (fail closed, RN-09). |

### Paginación / Filtros / Ordenamiento
No aplica.

### Restricciones del contrato
El campo `provider` es **obligatorio** en el request; el sistema no autodetecta el proveedor a partir del `username` (`ADR-018`). Esto evita ambigüedad entre los namespaces de `LOCAL` y `ACTIVE_DIRECTORY` (INV-AUTH-001) y elimina cualquier lógica de "probar un proveedor y luego otro".

## 13. Referencias de dominio
- Entidad: User, PasswordCredential, UserRoleAssignment, AdGroupRoleMapping
- Invariante: INV-AUTH-001, INV-AUTH-002, INV-AUTH-003, INV-AUTH-004, INV-AUTH-005, INV-AUTH-010
- Estado: PENDING_ONBOARDING, ACTIVE, LOCKED, DISABLED
- Transición: ver `02-domain/transitions.md`

## 14. Trazabilidad
- Requisitos: REQ-AUTH-001, REQ-AUTH-002, REQ-AUTH-003, REQ-AUTH-004, REQ-AUTH-005, REQ-AUTH-007, REQ-AUTH-008, REQ-AUTH-018, REQ-AUTH-025
- Dominio: User, AdGroupRoleMapping, UserRoleAssignment
- Pruebas: TEST-AUTH-001 y siguientes (ver `testing-strategy.md §3`)
- ADR: ADR-002, ADR-003, ADR-004, ADR-005, ADR-017, ADR-018

## 15. GAPs
Sin GAPs abiertos. GAP-AUTH-001 (fail closed ante fallo de sincronización AD) y GAP-AUTH-002 (provider explícito) fueron resueltos por el Decision Ledger del 2026-09-05 e incorporados en `ADR-017` y `ADR-018` respectivamente.
