# SPEC-AUTH-003 — Cierre de sesión

**Estado:** APPROVED
**Versión:** 1.0

## 1. Objetivo
Permitir que un usuario finalice su sesión activa, o todas sus sesiones en todos los dispositivos.

## 2. Alcance
### Incluye
Logout individual (revoca una familia de Refresh Token) y logout-all (revoca todas las familias del usuario).
### No incluye
Invalidación inmediata del Access Token ya emitido (expira naturalmente, ver ADR-006). Revocación disparada por un administrador sobre otro usuario (`SPEC-AUTH-004`).

## 3. Actores
Usuario autenticado.

## 4. Requisitos relacionados
REQ-AUTH-010

## 5. Reglas de negocio
- RN-01: Logout individual revoca únicamente la familia del Refresh Token presentado (la sesión/dispositivo actual).
- RN-02: Logout-all revoca todas las familias de Refresh Token vigentes del usuario autenticado.
- RN-03: Ninguna de las dos operaciones invalida de forma inmediata los Access Tokens ya emitidos; estos expiran naturalmente dentro de su ventana corta (máximo 15 minutos).

## 6. Casos de uso

### UC-AUTH-006 — Cerrar sesión

**Actor:** Usuario autenticado.
**Precondiciones:** Sesión activa con Refresh Token vigente.
**Flujo principal:**
1. El usuario solicita logout.
2. El sistema revoca la familia del Refresh Token presentado (sin reemplazo).
3. El sistema registra `LOGOUT`.
4. El sistema instruye al cliente a eliminar la cookie de Refresh Token.
**Flujos alternativos:** Refresh Token ya inválido/ausente → la operación se considera igualmente exitosa (idempotente: el objetivo — "no tener sesión activa" — ya se cumple).
**Errores:** ninguno bloqueante (idempotente).
**Resultado esperado:** la sesión/dispositivo actual queda sin Refresh Token válido.

### UC-AUTH-007 — Cerrar todas las sesiones

**Actor:** Usuario autenticado.
**Precondiciones:** Sesión activa.
**Flujo principal:**
1. El usuario solicita logout-all.
2. El sistema revoca todas las familias de Refresh Token vigentes del usuario.
3. El sistema registra `LOGOUT_ALL`.
**Flujos alternativos:** ninguno relevante.
**Errores:** ninguno bloqueante.
**Resultado esperado:** ningún Refresh Token del usuario permanece vigente en ningún dispositivo.

## 7. Casos límite
Logout llamado dos veces seguidas (idempotencia) → ambas responden éxito, la segunda sin efecto adicional.

## 8. Criterios de aceptación

```gherkin
Feature: Cierre de sesión

  Scenario: Logout individual
    Given un usuario con una sesión activa
    When solicita cerrar sesión
    Then su Refresh Token de esa sesión queda revocado
    And se registra un evento LOGOUT

  Scenario: Logout de todos los dispositivos
    Given un usuario con sesiones activas en múltiples dispositivos
    When solicita cerrar sesión en todos los dispositivos
    Then todas sus familias de Refresh Token quedan revocadas
    And se registra un evento LOGOUT_ALL

  Scenario: Logout es idempotente
    Given un usuario que ya cerró sesión
    When repite la solicitud de logout con el mismo Refresh Token ya revocado
    Then el sistema responde éxito sin generar un error
```

## 9. Dependencias
`TokenPort`, `UserRepositoryPort`, `AuditPort`.

## 10. Restricciones
Ninguna adicional.

## 11. Seguridad
Ver `SPEC-AUTH-002 §11` y ADR-010 para el manejo de la cookie.

## 12. API Contract

### Método / Endpoint
`POST /auth/logout` y `POST /auth/logout/all`

### Autenticación
Basta con el Refresh Token vía cookie (decisión de implementación, Fase 11 — parámetro de bajo impacto y reversible, `.claude/core/decision-authority.md`, no requiere ADR propio): el propio Refresh Token ya identifica unívocamente la familia (logout) o el usuario (logout-all) a revocar, sin necesitar verificar el Access Token — igual criterio que `POST /auth/refresh` (SPEC-AUTH-002). Un Refresh Token ausente o inválido no bloquea la operación (RN de idempotencia, ver flujos alternativos de UC-AUTH-006/007): en ese caso no hay nada que revocar, y la respuesta es igualmente 200.

### Autorización
Ninguna más allá de estar autenticado sobre la propia sesión.

### Request
Sin cuerpo en ambos casos.

### Response (200 OK)
```json
{ "status": "ok" }
```

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200 | OK | Logout (individual o total) ejecutado, incluso si no había sesión vigente (idempotente). |

### Errores (RFC 7807)
No se definen errores propios más allá de los genéricos de autenticación (401 si no hay ninguna credencial reconocible).

### Restricciones del contrato
`/auth/logout/all` debe indicar explícitamente en su documentación de frontend que cierra sesión en **todos** los dispositivos, para evitar uso accidental.

## 13. Referencias de dominio
- Entidad: RefreshToken, RefreshTokenFamily

## 14. Trazabilidad
- Requisitos: REQ-AUTH-010
- ADR: ADR-007, ADR-008, ADR-010
