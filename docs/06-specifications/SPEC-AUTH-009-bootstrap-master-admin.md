# SPEC-AUTH-009 — Bootstrap del administrador local (Master Admin)

**Estado:** APPROVED
**Versión:** 1.2

## 1. Objetivo
Garantizar que exista al menos una cuenta administrativa `LOCAL`, capaz de operar la aplicación aunque Active Directory esté completamente indisponible, sin introducir una contraseña embebida en el código como vulnerabilidad.

## 2. Alcance
### Incluye
Creación de la cuenta administrativa inicial en el arranque de la aplicación, a partir de un secreto externo (no del código fuente); obligación de cambio de contraseña en el primer uso; protección contra quedar sin ningún administrador local activo.
### No incluye
Gestión ordinaria de administradores adicionales (se realiza mediante las capacidades administrativas generales del sistema, no mediante este flujo de bootstrap).

## 3. Actores
Operador/proceso de arranque de la aplicación (no un usuario final vía API).

## 4. Requisitos relacionados
REQ-AUTH-016, REQ-AUTH-019

## 5. Reglas de negocio
- RN-01: El sistema nunca contiene una contraseña administrativa en texto plano dentro del código fuente ni en un valor por defecto predecible.
- RN-02: El bootstrap consume un secreto externo (hash pre-generado, inyectado mediante variable de entorno o gestor de secretos) únicamente si no existe ya ningún `User` `LOCAL` con rol administrativo.
- RN-03: La cuenta resultante del bootstrap tiene `mustChangeOnNextLogin = true`; no puede realizar ninguna acción administrativa hasta cambiar su contraseña.
- RN-04: El sistema no debe permitir, mediante un flujo normal de administración, que la última cuenta administrativa local activa quede deshabilitada o eliminada.
- RN-05: El bloqueo por intentos fallidos sobre esta cuenta nunca es un bloqueo permanente, para evitar que sea utilizada como vector de denegación de servicio contra el único acceso de break-glass: transcurrido el cooldown, la cuenta puede volver a intentar autenticarse sin depender de la intervención de otro administrador. **Decision Ledger 2026-09-06:** implementado con el mismo cooldown de duración fija que aplica al resto de los usuarios `LOCAL` (RN-07 SPEC-AUTH-001) — no con un cooldown creciente/escalonado; esa variante queda fuera de alcance de V1 (ver §15). El auto-desbloqueo por cooldown expirado aplica únicamente a la cuenta que posee el Role `MASTER_ADMIN`; el resto de los usuarios `LOCKED` sigue requiriendo desbloqueo administrativo explícito (`ChangeUserStatusUseCase.UNLOCK`), conforme a RN-07 SPEC-AUTH-001, que no exige lo contrario.
- RN-06: Todo el proceso de bootstrap se audita (`MASTER_ADMIN_BOOTSTRAPPED`).
- RN-07: Si al arrancar no existe ningún administrador local y tampoco hay un secreto externo de bootstrap válido configurado, la aplicación falla rápidamente (fail-fast) y no continúa su arranque; nunca queda operativa sin ningún mecanismo administrativo disponible (`ADR-019`).
- RN-08: La protección contra deshabilitar, degradar o eliminar al último administrador local activo es una validación transaccional y bloqueante en el momento de la operación, no una alerta posterior. Cuando sea necesario retirar una cuenta administrativa, se prefiere soft-deletion/deprovisioning (transición a `DEPROVISIONED`) frente a la eliminación física, para preservar el historial de auditoría (`ADR-019`).
- RN-09: Una vez completado el cambio de contraseña obligatorio (RN-03), la cuenta deja de estar en modo bootstrap y se comporta como cualquier administrador local ordinario; el secreto externo original de bootstrap no vuelve a utilizarse.

## 6. Casos de uso

### UC-AUTH-014 — Bootstrap del administrador local

**Actor:** Proceso de arranque de la aplicación.
**Precondiciones:** No existe ningún `User` `LOCAL` con rol administrativo.
**Flujo principal:**
1. Al iniciar, el sistema verifica si existe algún administrador local.
2. Si no existe, el sistema lee el secreto externo configurado (hash de la contraseña inicial).
3. El sistema crea el `User` `LOCAL` administrativo con `credential` a partir de ese hash y `mustChangeOnNextLogin = true`.
4. El sistema registra `MASTER_ADMIN_BOOTSTRAPPED`.
**Flujos alternativos:**
- 2a. No se proporcionó secreto externo y no existe administrador → **resuelto: fail-fast** (RN-07, `ADR-019`). La aplicación aborta el arranque con un error claro; no continúa operativa sin ningún mecanismo administrativo disponible.
- Ya existe un administrador local → el bootstrap no hace nada (idempotente).
**Errores:** secreto externo ausente o con formato inválido.
**Resultado esperado:** garantía de al menos un administrador local funcional tras el primer arranque, sin secretos en el código.

## 7. Casos límite
- Un administrador intenta deshabilitar, degradar o eliminar la última cuenta administrativa local activa → **resuelto**: rechazo mediante validación transaccional y bloqueante en el momento de la operación (RN-08); se prefiere soft-deletion/deprovisioning sobre eliminación física.
- Múltiples instancias arrancando simultáneamente (despliegue multi-instancia) intentando el bootstrap al mismo tiempo → debe existir una única creación efectiva (constraint de unicidad + manejo de conflicto de inserción), no dos administradores con el mismo secreto o un error no controlado.

## 8. Criterios de aceptación

```gherkin
Feature: Bootstrap del administrador local

  Scenario: Primer arranque sin administrador existente
    Given que no existe ningún administrador local
    And se configuró un secreto externo válido
    When la aplicación arranca
    Then se crea la cuenta administrativa local
    And esa cuenta requiere cambio de contraseña en su primer uso
    And se registra un evento MASTER_ADMIN_BOOTSTRAPPED

  Scenario: Arranque con administrador ya existente
    Given que ya existe un administrador local
    When la aplicación arranca nuevamente
    Then no se crea ninguna cuenta adicional
    And no se registra un nuevo evento de bootstrap

  Scenario: Protección contra quedar sin administrador
    Given que existe exactamente un administrador local activo
    When se intenta deshabilitar esa única cuenta
    Then la operación se rechaza

  Scenario: Cooldown no permanente ante intentos fallidos
    Given la cuenta administrativa local
    When se exceden los intentos fallidos de autenticación permitidos
    Then la cuenta entra en un cooldown temporal, no en un bloqueo permanente

  Scenario: Fail-fast sin administrador ni secreto de bootstrap
    Given que no existe ningún administrador local
    And no se configuró ningún secreto externo de bootstrap
    When la aplicación intenta arrancar
    Then el arranque falla explícitamente
    And la aplicación no queda operativa

  Scenario: La credencial de bootstrap deja de ser válida tras el cambio obligatorio
    Given la cuenta administrativa recién aprovisionada por bootstrap
    When completa el cambio de contraseña obligatorio en su primer uso
    Then la cuenta pasa a comportarse como un administrador local ordinario
    And el secreto externo original de bootstrap no vuelve a utilizarse
```

## 9. Dependencias
`UserRepositoryPort`, `PasswordHasherPort`, `AuditPort`, mecanismo de configuración externa (variable de entorno / gestor de secretos — infraestructura).

## 10. Restricciones
Ninguna adicional.

## 11. Seguridad
Ver `docs/03-architecture/security.md §7` (riesgo de compromiso de la única cuenta administrativa local).

## 12. API Contract
No aplica: esta capacidad se ejecuta durante el arranque de la aplicación (proceso interno/operativo), no mediante un endpoint HTTP consumido por el frontend (conforme a `.claude/documentation/templates.md §4, regla 13`).

## 13. Referencias de dominio
- Entidad: User, PasswordCredential
- Invariante: INV-AUTH-003

## 14. Trazabilidad
- Requisitos: REQ-AUTH-016, REQ-AUTH-019
- ADR: ADR-011, ADR-019

## 15. GAPs
Sin GAPs abiertos. GAP-AUTH-006 (fail-fast en bootstrap) y GAP-AUTH-007 (protección del último administrador) fueron resueltos por el Decision Ledger del 2026-09-05 e incorporados en `ADR-019`.

GAP-AUTH-008 (cooldown creciente de RN-05, Fase 20/Testing): la implementación V1 usa cooldown de duración fija (idéntico al de usuarios `LOCAL` ordinarios), no un cooldown creciente/escalonado por bloqueos consecutivos. Resuelto por el Decision Ledger del 2026-09-06 como simplificación aceptada para V1 (ver RN-05) — no bloquea `IMPLEMENTATION COMPLETE`; queda documentado como limitación conocida para una eventual V2.
