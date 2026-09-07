# SPEC-AUTH-006 — Autorización de solicitudes protegidas

**Estado:** APPROVED
**Versión:** 1.0

## 1. Objetivo
Definir cómo el sistema decide si una solicitud protegida se permite o se deniega, a partir del estado, roles y permisos vigentes del usuario.

## 2. Alcance
### Incluye
Evaluación RBAC + Permissions, verificación de `User.status`, semántica aditiva sin reglas DENY.
### No incluye
ABAC, autorización por atributo/recurso/ownership, políticas contextuales (postergado explícitamente).

## 3. Actores
No aplica un actor humano explícito: esta capacidad es un mecanismo transversal aplicado a toda solicitud protegida, sin una interacción actor-sistema orientada a un objetivo propio. Por esta razón, **no se define un Caso de Uso** para esta SPEC (conforme a `.claude/documentation/conventions.md §7`: "no es obligatorio crear un UC cuando la capacidad no requiere una interacción actor-sistema explícita").

## 4. Requisitos relacionados
REQ-AUTH-006, REQ-AUTH-015

## 5. Reglas de negocio
- RN-01: Una solicitud protegida se autoriza si y solo si el usuario, mediante al menos uno de sus roles vigentes, posee el permiso requerido por la acción.
- RN-02: La ausencia del permiso requerido deniega la solicitud; no existen reglas de denegación explícita (DENY) que puedan anular un permiso presente.
- RN-03: Antes de evaluar permisos, se verifica que `User.status = ACTIVE`. Ningún otro estado permite autorización, independientemente de los permisos asociados a sus roles.
- RN-04: La evaluación se realiza contra el estado vigente en base de datos en el momento de la solicitud (INV-AUTH-012), nunca exclusivamente contra claims congelados del Access Token.

## 6. Casos de uso
No aplica (ver §3).

## 7. Casos límite
- Un usuario posee dos roles con permisos superpuestos → la unión de permisos de todos sus roles determina el resultado (semántica aditiva, sin conflicto posible al no existir DENY).
- Un usuario pierde su único rol entre la emisión del Access Token y una solicitud posterior dentro de la ventana de vigencia del token → la solicitud se deniega igualmente, porque la evaluación es contra base de datos (RN-04), no contra el token.

## 8. Criterios de aceptación

```gherkin
Feature: Autorización de solicitudes protegidas

  Scenario: Autorización concedida
    Given un usuario ACTIVE con un rol que posee el permiso requerido
    When realiza una solicitud protegida por ese permiso
    Then la solicitud se autoriza

  Scenario: Autorización denegada por falta de permiso
    Given un usuario ACTIVE sin el permiso requerido en ninguno de sus roles
    When realiza una solicitud protegida por ese permiso
    Then la solicitud se deniega con 403
    And se registra un evento AUTHORIZATION_DENIED

  Scenario: Cuenta no activa con token criptográficamente válido
    Given un usuario cuyo status es DISABLED
    And posee un Access Token todavía no expirado
    When realiza una solicitud protegida
    Then la solicitud se deniega, independientemente de sus permisos previos

  Scenario: Cambio de permisos con efecto inmediato
    Given un usuario ACTIVE con un permiso concedido
    When un administrador le retira ese permiso
    And el usuario repite la misma solicitud protegida sin obtener un nuevo token
    Then la solicitud se deniega desde esa siguiente solicitud
```

## 9. Dependencias
`UserRepositoryPort`. Consumida por prácticamente todas las demás SPECs de este módulo y, en el futuro, por cualquier capacidad protegida de otros módulos del sistema.

## 10. Restricciones
No implementar ABAC ni policy engine en V1 (ver exclusiones del módulo).

## 11. Seguridad
Ver ADR-006 (DB como fuente de verdad) y ADR-015 (mecanismo: servicio de aplicación explícito, no Decorator).

## 12. API Contract
No aplica: esta SPEC define un mecanismo transversal, no un endpoint propio consumido directamente por el frontend (conforme a `.claude/documentation/templates.md §4, regla 13`: "cuando una Specification no sea expuesta mediante API, la sección API Contract no debe agregarse").

## 13. Referencias de dominio
- Entidad: User, Role, Permission, UserRoleAssignment
- Invariante: INV-AUTH-004, INV-AUTH-011, INV-AUTH-012
- Estado: PENDING_ONBOARDING, ACTIVE, LOCKED, DISABLED, DEPROVISIONED

## 14. Trazabilidad
- Requisitos: REQ-AUTH-006, REQ-AUTH-015
- ADR: ADR-006, ADR-015
