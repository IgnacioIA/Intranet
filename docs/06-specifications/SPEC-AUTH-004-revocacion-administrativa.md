# SPEC-AUTH-004 — Revocación administrativa de sesiones

**Estado:** APPROVED
**Versión:** 1.1

## 1. Objetivo
Permitir que un administrador autorizado revoque todas las sesiones activas de un usuario determinado, para responder a incidentes de seguridad sin intervención directa en base de datos.

## 2. Alcance
### Incluye
Revocación de todas las familias de Refresh Token de un usuario objetivo, ejecutada por un administrador.
### No incluye
Autoservicio del propio usuario (`SPEC-AUTH-003`, UC-AUTH-007). Invalidación inmediata de Access Tokens ya emitidos.

## 3. Actores
Administrador con permiso `SESSION_REVOKE_ANY`.

## 4. Requisitos relacionados
REQ-AUTH-011

## 5. Reglas de negocio
- RN-01: Solo un actor con el permiso `SESSION_REVOKE_ANY` puede ejecutar esta operación sobre un usuario distinto de sí mismo.
- RN-02: La operación debe auditarse identificando tanto al administrador (actor) como al usuario afectado (sujeto).

## 6. Casos de uso

### UC-AUTH-008 — Revocar sesiones de un usuario (administrativo)

**Actor:** Administrador.
**Precondiciones:** El actor posee `SESSION_REVOKE_ANY`; el usuario objetivo existe.
**Flujo principal:**
1. El administrador solicita revocar las sesiones del usuario objetivo.
2. El sistema verifica el permiso del actor (`SPEC-AUTH-006`).
3. El sistema revoca todas las familias de Refresh Token vigentes del usuario objetivo.
4. El sistema registra `ADMIN_SESSION_REVOCATION` con actor y sujeto.
**Flujos alternativos:** el usuario objetivo no tiene sesiones vigentes → operación igualmente exitosa (idempotente).
**Errores:** permiso insuficiente (403), usuario objetivo inexistente (404).
**Resultado esperado:** el usuario objetivo pierde toda capacidad de renovar sesión desde ese momento; sus Access Tokens ya emitidos expiran naturalmente dentro de su ventana corta.

## 7. Casos límite
Un administrador intenta revocar sus propias sesiones mediante este endpoint administrativo → debería redirigirse conceptualmente al flujo de autoservicio (`UC-AUTH-007`), aunque el resultado final sea equivalente; no se considera un error bloqueante.

## 8. Criterios de aceptación

```gherkin
Feature: Revocación administrativa de sesiones

  Scenario: Revocación exitosa
    Given un administrador con permiso SESSION_REVOKE_ANY
    And un usuario objetivo con sesiones activas
    When el administrador revoca las sesiones del usuario objetivo
    Then todas las familias de Refresh Token del usuario objetivo quedan revocadas
    And se registra un evento ADMIN_SESSION_REVOCATION con el administrador como actor y el usuario como sujeto

  Scenario: Revocación denegada por falta de permiso
    Given un usuario autenticado sin el permiso SESSION_REVOKE_ANY
    When intenta revocar las sesiones de otro usuario
    Then el sistema deniega la operación con 403
    And se registra un evento AUTHORIZATION_DENIED
```

## 9. Dependencias
`TokenPort`, `UserRepositoryPort`, `AuditPort`, `SPEC-AUTH-006` (evaluación del permiso).

## 10. Restricciones
Ninguna adicional.

## 11. Seguridad
Esta es una capacidad administrativa sensible: por política de auditoría (`docs/03-architecture/security.md §5`, resuelta en el Decision Ledger), se audita tanto `AUTHORIZATION_DENIED` como `AUTHORIZATION_GRANTED` para esta operación — a diferencia del tráfico general, donde solo se audita lo denegado.

## 12. API Contract

### Método
`POST`

### Endpoint
`/auth/admin/users/{userId}/revoke-sessions`

### Autenticación
Access Token vigente.

### Autorización
Permiso `SESSION_REVOKE_ANY`.

### Parámetros de ruta
| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `userId` | Identificador | Sí | Usuario objetivo. |

### Request
Sin cuerpo.

### Response (200 OK)
```json
{ "status": "ok", "revokedSessions": 3 }
```

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200 | OK | Revocación ejecutada (idempotente si no había sesiones). |
| 403 | Forbidden | Actor sin el permiso requerido. |
| 404 | Not Found | Usuario objetivo inexistente. |

### Errores (RFC 7807)
| Código | Error (`type`) | Condición |
|---|---|---|
| 403 | `insufficient-permissions` | — |
| 404 | `user-not-found` | — |

### Restricciones del contrato
Consumido por un panel de administración, no por el flujo general de usuario final — igualmente documentado como contrato público porque un frontend administrativo lo invoca directamente.

## 13. Referencias de dominio
- Entidad: RefreshToken, User

## 14. Trazabilidad
- Requisitos: REQ-AUTH-011
- Especificaciones: SPEC-AUTH-006 (evaluación del permiso `SESSION_REVOKE_ANY`)
