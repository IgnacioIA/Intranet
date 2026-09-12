# SPEC-AUTH-008 — Administración del mapping AD Group → Application Role

**Estado:** APPROVED
**Versión:** 1.2

## 1. Objetivo
Permitir que un administrador autorizado mantenga, de forma auditada, la única fuente que traduce pertenencia a un grupo de Active Directory en un rol de la aplicación.

## 2. Alcance
### Incluye
Alta, baja y modificación de `AdGroupRoleMapping`, con auditoría de autor y fecha.
### No incluye
Interpretación automática de la estructura de grupos de AD; sincronización masiva o descubrimiento automático de grupos existentes en AD (fuera de alcance de V1).

## 3. Actores
Administrador con permiso `AD_MAPPING_MANAGE`.

## 4. Requisitos relacionados
REQ-AUTH-004, REQ-AUTH-016

## 5. Reglas de negocio
- RN-01: Solo un actor con `AD_MAPPING_MANAGE` puede crear, modificar o eliminar un `AdGroupRoleMapping`.
- RN-02: `adGroupIdentifier` es único (INV-AUTH-010); no puede crearse un segundo mapping para el mismo grupo sin antes eliminar o modificar el existente.
- RN-03: Toda operación de alta/baja/modificación se audita con el administrador autor y la marca de tiempo (`AD_MAPPING_CHANGED`).
- RN-04: Eliminar un mapping no revoca retroactivamente los roles ya derivados de él; el efecto se aplica en la siguiente sincronización de cada usuario afectado (`SPEC-AUTH-001`, UC-AUTH-004), de forma consistente con que los roles derivados se recalculan en cada login, no en tiempo real fuera de ese proceso.
- RN-05 (implementación, cierre de gap 2026-09-07): el `roleId` de un mapping (alta o modificación) debe referenciar un `Role` existente y **activo**. Un `Role` inexistente rechaza con 404 (`role-not-found`); un `Role` inactivo rechaza con el mismo error contractual ya usado por `SPEC-AUTH-010` (UC-AUTH-021) para el mismo tipo de violación — `RoleInactiveException`, 400 `role-inactive` — en vez de introducir un código de error paralelo para el mismo concepto de negocio ("no se puede referenciar un Role inactivo").

## 6. Casos de uso

### UC-AUTH-013 — Administrar mapping AD Group → Role

**Actor:** Administrador.
**Precondiciones:** El actor posee `AD_MAPPING_MANAGE`.
**Flujo principal (alta):**
1. El administrador envía el identificador del grupo AD y el rol destino.
2. El sistema verifica que no exista ya un mapping para ese grupo (RN-02).
3. El sistema crea el `AdGroupRoleMapping`.
4. El sistema registra `AD_MAPPING_CHANGED` (alta).
**Flujos alternativos:**
- Modificación: el administrador cambia el `roleId` de un mapping existente → se actualiza y se audita igualmente.
- Baja: el administrador elimina un mapping → se elimina y se audita; efecto diferido según RN-04.
- 2a. Ya existe mapping para ese grupo → 409 Conflict.
**Errores:** permiso insuficiente, grupo ya mapeado (alta), mapping inexistente (baja/modificación).
**Resultado esperado:** el mapping vigente refleja exactamente las decisiones administrativas tomadas, con historial auditable de quién las tomó.

## 7. Casos límite
Eliminar el único mapping que sostiene el estado `ACTIVE` de varios usuarios → no se revierte nada de forma inmediata (RN-04); esos usuarios pasarán a `PENDING_ONBOARDING` recién en su próximo login, lo cual es el comportamiento esperado y no requiere ninguna acción adicional de esta SPEC.

## 8. Criterios de aceptación

```gherkin
Feature: Administración del mapping AD Group → Role

  Scenario: Alta de mapping exitosa
    Given un administrador con permiso AD_MAPPING_MANAGE
    And ningún mapping previo para el grupo "IT-SUPPORT"
    When crea un mapping de "IT-SUPPORT" hacia el rol "CONTENT_EDITOR"
    Then el mapping queda creado
    And se registra un evento AD_MAPPING_CHANGED con el administrador como autor

  Scenario: Alta duplicada rechazada
    Given un mapping existente para el grupo "IT-SUPPORT"
    When un administrador intenta crear otro mapping para el mismo grupo
    Then el sistema rechaza la operación con 409

  Scenario: Administración denegada por falta de permiso
    Given un usuario autenticado sin el permiso AD_MAPPING_MANAGE
    When intenta crear un mapping
    Then el sistema deniega la operación con 403
```

## 9. Dependencias
`UserRepositoryPort` (para `Role`), `AuditPort`, `SPEC-AUTH-006`.

## 10. Restricciones
Ninguna adicional.

## 11. Seguridad
Esta capacidad es de alto impacto (condiciona autorización de todos los usuarios AD): auditoría obligatoria de toda operación, sin excepción. Por ser una operación administrativa sensible, se audita también `AUTHORIZATION_GRANTED` además de `AUTHORIZATION_DENIED` (`docs/03-architecture/security.md §5`).

## 12. API Contract

### Método / Endpoint
`GET /auth/admin/ad-group-mappings` · `POST /auth/admin/ad-group-mappings` · `PUT /auth/admin/ad-group-mappings/{id}` · `DELETE /auth/admin/ad-group-mappings/{id}`

### Autenticación
Access Token vigente.

### Autorización
Permiso `AD_MAPPING_MANAGE`.

### Request (POST/PUT)
```json
{ "adGroupIdentifier": "IT-SUPPORT", "roleId": "role-content-editor" }
```

### Response (200/201)
```json
{ "id": "map-001", "adGroupIdentifier": "IT-SUPPORT", "roleId": "role-content-editor", "updatedAt": "2026-09-04T12:00:00Z" }
```

### Códigos HTTP
| Código | Significado | Condición |
|---|---|---|
| 200/201 | OK/Created | Operación exitosa. |
| 204 | No Content | Baja exitosa (DELETE). |
| 400 | Bad Request | `roleId` referencia un Role inactivo (RN-05). |
| 403 | Forbidden | Sin permiso `AD_MAPPING_MANAGE`. |
| 404 | Not Found | Mapping inexistente (PUT/DELETE) o Role inexistente (POST/PUT, RN-05). |
| 409 | Conflict | Grupo ya mapeado (POST/PUT). |

### Errores (RFC 7807)
| Código | Error (`type`) | Condición |
|---|---|---|
| 400 | `role-inactive` | RN-05 — mismo error contractual que `SPEC-AUTH-010` UC-AUTH-021. |
| 403 | `insufficient-permissions` | — |
| 404 | `mapping-not-found` | — |
| 404 | `role-not-found` | RN-05. |
| 409 | `group-already-mapped` | También aplica al PUT si el nuevo `adGroupIdentifier` ya pertenece a otro mapping. |

### Restricciones del contrato
Consumido por un panel de administración; no forma parte del flujo de usuario final.

## 13. Referencias de dominio
- Entidad: AdGroupRoleMapping, Role
- Invariante: INV-AUTH-010

## 14. Trazabilidad
- Requisitos: REQ-AUTH-004, REQ-AUTH-016
- Dominio: AdGroupRoleMapping
- ADR: ADR-005
