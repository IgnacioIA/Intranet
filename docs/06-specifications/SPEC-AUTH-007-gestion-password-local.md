# SPEC-AUTH-007 — Gestión de contraseña de usuarios locales

**Estado:** APPROVED
**Versión:** 1.1

## 1. Objetivo
Permitir a un usuario `LOCAL` cambiar su contraseña estando autenticado, y recuperarla mediante un proceso de dos pasos cuando la haya olvidado, sin exponer información que permita enumerar cuentas.

## 2. Alcance
### Incluye
Cambio de contraseña autenticado; solicitud y confirmación de recuperación mediante token de un solo uso; rechazo explícito de ambas operaciones para usuarios `ACTIVE_DIRECTORY`.
### No incluye
Cambio o recuperación de contraseña de Active Directory (REQ-AUTH-012).

## 3. Actores
Usuario `LOCAL`.

## 4. Requisitos relacionados
REQ-AUTH-012, REQ-AUTH-013, REQ-AUTH-019, REQ-AUTH-025

## 5. Reglas de negocio
- RN-01: Un usuario `ACTIVE_DIRECTORY` que solicita cambio o recuperación de contraseña recibe un rechazo orientativo (indicando el canal de AD/IT correspondiente), sin exponer detalles internos del sistema.
- RN-02: La solicitud de recuperación responde de forma equivalente exista o no una cuenta asociada al correo indicado (RN de no enumeración, REQ-AUTH-025).
- RN-03: Un token de recuperación es de un solo uso y expira (INV-AUTH-008); al confirmarse exitosamente, se marca usado y se invalida cualquier otro token de recuperación pendiente del mismo usuario.
- RN-04: Toda nueva contraseña se almacena mediante Argon2id (ADR-011); nunca se almacena ni se transmite en logs/auditoría.
- RN-05: Un cambio o recuperación de contraseña exitosos revocan todas las sesiones activas del usuario (todas las familias de Refresh Token), forzando nuevo login en todos los dispositivos — mitigación ante la hipótesis de que la contraseña anterior estuviera comprometida.
- RN-06: Toda nueva solicitud de recuperación exitosa (UC-AUTH-011) invalida cualquier `PasswordRecoveryToken` pendiente previo del mismo usuario, de forma que exista a lo sumo un token de recuperación válido por usuario en todo momento.

## 6. Casos de uso

### UC-AUTH-010 — Cambiar contraseña

**Actor:** Usuario `LOCAL` autenticado.
**Precondiciones:** Usuario autenticado con `provider = LOCAL`.
**Flujo principal:**
1. El usuario envía su contraseña actual y la nueva contraseña.
2. El sistema verifica la contraseña actual.
3. El sistema valida la nueva contraseña contra la política vigente: **longitud mínima de 12 caracteres, sin reglas arbitrarias de complejidad** (parámetro fijado en Fase 3 de implementación — `PasswordPolicy`, decisión de bajo impacto y reversible según `.claude/core/decision-authority.md`, no requiere ADR).
4. El sistema almacena el nuevo hash Argon2id.
5. El sistema revoca todas las sesiones activas del usuario (RN-05).
6. El sistema registra `PASSWORD_CHANGED`.
**Flujos alternativos:**
- 1a. Actor con `provider = ACTIVE_DIRECTORY` → rechazo orientativo (RN-01); no se registra como intento fallido de seguridad, sino como un uso indebido del endpoint documentado.
- 2a. Contraseña actual incorrecta → 401/403 genérico; no revocar sesiones.
**Errores:** contraseña actual incorrecta, nueva contraseña no cumple política.
**Resultado esperado:** contraseña actualizada; todas las sesiones previas invalidadas.

### UC-AUTH-011 — Solicitar recuperación de contraseña

**Actor:** Usuario `LOCAL` (no autenticado).
**Precondiciones:** Ninguna verificable por el solicitante (por diseño, ver RN-02).
**Flujo principal:**
1. El usuario envía su email.
2. El sistema busca un `User(provider=LOCAL, email=...)`.
3. Si existe → invalida cualquier `PasswordRecoveryToken` pendiente previo del usuario (RN-06), genera un nuevo `PasswordRecoveryToken` y lo envía por correo (fuera del alcance técnico de esta SPEC: el envío de correo es infraestructura, no comportamiento de dominio).
4. El sistema responde de forma idéntica exista o no la cuenta (RN-02).
5. El sistema registra `PASSWORD_RECOVERY_REQUESTED` (sin registrar si la cuenta existía, para no crear una fuga de esa información ni siquiera en auditoría interna más allá de lo estrictamente necesario para operación).
**Flujos alternativos:**
- Actor con `provider = ACTIVE_DIRECTORY` asociado a ese email → mismo comportamiento externo que "cuenta inexistente" (RN-01 + RN-02 combinadas: tampoco se revela que el email pertenece a una cuenta AD).
**Errores:** ninguno expuesto externamente.
**Resultado esperado:** respuesta uniforme; si la cuenta existe y es `LOCAL`, recibe un correo con el token.

### UC-AUTH-012 — Confirmar recuperación de contraseña

**Actor:** Usuario `LOCAL`.
**Precondiciones:** Posee un `PasswordRecoveryToken` no usado y no expirado.
**Flujo principal:**
1. El usuario envía el token y la nueva contraseña.
2. El sistema valida el token (no usado, no expirado, INV-AUTH-008).
3. El sistema valida la nueva contraseña contra la política vigente.
4. El sistema actualiza el hash, marca el token como usado.
5. El sistema revoca todas las sesiones activas del usuario (RN-05).
6. El sistema registra `PASSWORD_RECOVERY_CONFIRMED`.
**Flujos alternativos:**
- 2a. Token inválido/expirado/ya usado → error genérico (no distinguir cuál de las tres condiciones ocurrió).
**Errores:** token inválido, nueva contraseña no cumple política.
**Resultado esperado:** contraseña actualizada; token consumido; sesiones previas invalidadas.

## 7. Casos límite
- Solicitudes repetidas de recuperación para la misma cuenta → **resuelto** (RN-06): cada nueva solicitud exitosa invalida los tokens de recuperación previos pendientes de esa cuenta; solo el más reciente es válido.
- Rate limiting sobre la solicitud de recuperación (evitar abuso de envío masivo de correos) — cubierto transversalmente por REQ-AUTH-018, aplicado también a este endpoint.

## 8. Criterios de aceptación

```gherkin
Feature: Gestión de contraseña de usuarios locales

  Scenario: Cambio de contraseña exitoso
    Given un usuario LOCAL autenticado que conoce su contraseña actual
    When cambia su contraseña por una nueva válida
    Then la nueva contraseña queda almacenada mediante Argon2id
    And todas sus sesiones activas quedan revocadas
    And se registra un evento PASSWORD_CHANGED

  Scenario: Usuario AD intenta cambiar contraseña
    Given un usuario ACTIVE_DIRECTORY autenticado
    When intenta cambiar su contraseña mediante este módulo
    Then el sistema rechaza la operación orientándolo al canal de AD/IT

  Scenario: Solicitud de recuperación con cuenta existente
    Given un email asociado a un usuario LOCAL existente
    When se solicita recuperación de contraseña para ese email
    Then el usuario recibe un token de recuperación por correo
    And la respuesta HTTP es idéntica a la de un email inexistente

  Scenario: Solicitud de recuperación con cuenta inexistente
    Given un email que no corresponde a ninguna cuenta
    When se solicita recuperación de contraseña para ese email
    Then la respuesta HTTP es idéntica a la de una cuenta existente
    And no se envía ningún correo

  Scenario: Confirmación exitosa de recuperación
    Given un token de recuperación válido, no usado y no expirado
    When se confirma con una nueva contraseña válida
    Then la contraseña queda actualizada
    And el token queda marcado como usado
    And no puede reutilizarse

  Scenario: Confirmación con token ya usado
    Given un token de recuperación que ya fue utilizado
    When se intenta confirmar nuevamente con ese token
    Then el sistema rechaza la operación con un error genérico

  Scenario: Nueva solicitud de recuperación invalida la anterior
    Given un usuario LOCAL con un token de recuperación pendiente no expirado
    When solicita nuevamente la recuperación de su contraseña
    Then el token anterior queda invalidado
    And solo el nuevo token es válido para confirmar la recuperación
```

## 9. Dependencias
`UserRepositoryPort`, `PasswordHasherPort`, `AuditPort`, `RateLimiterPort`, y un mecanismo de envío de correo (infraestructura, fuera del dominio de este módulo).

## 10. Restricciones
Ninguna adicional.

## 11. Seguridad
No enumeración de usuarios (RN-02); revocación de sesiones tras cambio/recuperación (RN-05); rate limiting sobre solicitud de recuperación.

## 12. API Contract

### Método / Endpoint 1
`POST /auth/password/change`

**Autenticación:** Access Token vigente.
**Autorización:** ninguna adicional (solo sobre la propia cuenta).

**Request:**
```json
{ "currentPassword": "string", "newPassword": "string" }
```
**Response (200 OK):**
```json
{ "status": "ok" }
```
**Códigos HTTP:** 200 OK · 401/403 credencial actual incorrecta · 422 nueva contraseña no cumple política · 400 usuario AD (ver RN-01).
**Errores (RFC 7807):** `invalid-current-password`, `password-policy-violation`, `password-managed-externally` (usuario AD).

### Método / Endpoint 2
`POST /auth/password/recovery/request`

**Autenticación:** ninguna.
**Request:**
```json
{ "email": "user@example.com" }
```
**Response (202 Accepted):**
```json
{ "status": "ok" }
```
(202, no 200, para reforzar semánticamente "solicitud aceptada para procesamiento", sin confirmar resultado — refuerza RN-02.)
**Códigos HTTP:** 202 siempre, salvo error de validación de formato de email (400).
**Errores (RFC 7807):** `invalid-request` (formato de email inválido, no revela existencia de cuenta).

### Método / Endpoint 3
`POST /auth/password/recovery/confirm`

**Autenticación:** ninguna (el token de recuperación es la credencial).
**Request:**
```json
{ "token": "string", "newPassword": "string" }
```
**Response (200 OK):**
```json
{ "status": "ok" }
```
**Códigos HTTP:** 200 OK · 401 token inválido/expirado/usado (genérico) · 422 nueva contraseña no cumple política.
**Errores (RFC 7807):** `invalid-recovery-token`, `password-policy-violation`.

### Restricciones del contrato (comunes)
Ningún endpoint de esta SPEC debe revelar en su respuesta si la causa del rechazo fue "cuenta inexistente", "cuenta AD" o "token ya usado" de forma más específica de lo indicado arriba.

## 13. Referencias de dominio
- Entidad: User, PasswordCredential, PasswordRecoveryToken
- Invariante: INV-AUTH-003, INV-AUTH-008

## 14. Trazabilidad
- Requisitos: REQ-AUTH-012, REQ-AUTH-013, REQ-AUTH-019, REQ-AUTH-025
- ADR: ADR-011

## 15. GAPs
Sin GAPs abiertos. GAP-AUTH-005 (invalidación de tokens de recuperación previos) fue resuelto por el Decision Ledger del 2026-09-05 e incorporado como RN-06.
