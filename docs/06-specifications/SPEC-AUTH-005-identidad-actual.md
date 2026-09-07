# SPEC-AUTH-005 — Consulta de identidad y autorización actual

**Estado:** APPROVED
**Versión:** 1.1

## 1. Objetivo
Permitir que el frontend obtenga la identidad, estado y permisos vigentes del usuario autenticado, sin necesidad de interpretar directamente el contenido del Access Token.

## 2. Alcance
### Incluye
Identidad básica, estado de cuenta, roles y permisos efectivos.
### No incluye
Historial de sesiones o de auditoría del usuario (no forma parte de esta capacidad).

## 3. Actores
Usuario autenticado.

## 4. Requisitos relacionados
REQ-AUTH-014, REQ-AUTH-015

## 5. Reglas de negocio
- RN-01: La respuesta refleja el estado, roles y permisos vigentes en base de datos en el momento de la consulta, no un valor derivado únicamente del token (misma fuente de verdad que `SPEC-AUTH-006`).

## 6. Casos de uso

### UC-AUTH-009 — Consultar identidad actual

**Actor:** Usuario autenticado.
**Precondiciones:** Access Token vigente.
**Flujo principal:**
1. El cliente solicita su identidad actual.
2. El sistema resuelve el `User` a partir del Access Token.
3. El sistema consulta estado, roles y permisos vigentes.
4. El sistema responde con la representación completa.
**Flujos alternativos:** ninguno relevante.
**Errores:** Access Token ausente/expirado/inválido (401).
**Resultado esperado:** el frontend puede decidir qué mostrar/ocultar sin decodificar el JWT.

## 7. Casos límite
Un usuario en `PENDING_ONBOARDING` consulta su identidad → debe recibir su único rol (`ONBOARDING_USER`) y su único permiso (`VIEW_ONBOARDING_INFO`), permitiendo al frontend mostrar la pantalla de onboarding correspondiente.

## 8. Criterios de aceptación

```gherkin
Feature: Consulta de identidad actual

  Scenario: Usuario activo consulta su identidad
    Given un usuario autenticado y ACTIVE con roles asignados
    When consulta su identidad actual
    Then recibe su username, estado, roles y permisos vigentes

  Scenario: Usuario en onboarding consulta su identidad
    Given un usuario autenticado en estado PENDING_ONBOARDING
    When consulta su identidad actual
    Then recibe el rol ONBOARDING_USER y el permiso VIEW_ONBOARDING_INFO únicamente

  Scenario: Token inválido
    Given un Access Token expirado
    When se consulta la identidad actual con ese token
    Then el sistema responde 401
```

## 9. Dependencias
`UserRepositoryPort`, `TokenPort`.

## 10. Restricciones
La respuesta no debe incluir información sensible (contraseña, tokens) — solo identidad y autorización.

## 11. Seguridad
Ninguna consideración adicional a la autenticación estándar.

## 12. API Contract

### Método
`GET`

### Endpoint
`/auth/me`

### Autenticación
Access Token vigente.

### Autorización
Ninguna más allá de estar autenticado.

### Response (200 OK)
```json
{
  "id": "b3f1...",
  "provider": "ACTIVE_DIRECTORY",
  "username": "jdoe",
  "displayName": "Jane Doe",
  "email": "jdoe@example.com",
  "status": "ACTIVE",
  "roles": ["CONTENT_EDITOR"],
  "permissions": ["CONTENT_READ", "CONTENT_CREATE", "CONTENT_UPDATE"]
}
```

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200 | OK | — |
| 401 | Unauthorized | Token ausente/expirado/inválido. |

### Errores (RFC 7807)
| Código | Error (`type`) | Condición |
|---|---|---|
| 401 | `invalid-access-token` | — |

### Restricciones del contrato
`email` se incluye y puede ser `null` (ej. Shadow Identity AD sin correo mapeado). Es información de identidad, no una clave de autorización: ninguna decisión de acceso debe basarse en su valor.

## 13. Referencias de dominio
- Entidad: User, Role, Permission

## 14. Trazabilidad
- Requisitos: REQ-AUTH-014, REQ-AUTH-015
- Especificaciones: SPEC-AUTH-006 (misma fuente de verdad de autorización)

## 15. GAPs
Sin GAPs abiertos. GAP-AUTH-004 (inclusión de `email`) fue resuelto por el Decision Ledger del 2026-09-05: se incluye, nullable, sin valor de autorización.
