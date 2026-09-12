# Trazabilidad — Módulo de Seguridad (AUTH)

## 1. Propósito
Relacionar los Requirements, Specifications, dominio, pruebas y ADR del Módulo de Seguridad (contexto `AUTH`), conforme a `.claude/sdd/traceability.md`.

## 2. Requirement → Specification

| Requirement | Specification | Estado |
|---|---|---|
| REQ-AUTH-001 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-002 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-003 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-004 | SPEC-AUTH-001, SPEC-AUTH-008 | APPROVED |
| REQ-AUTH-005 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-006 | SPEC-AUTH-006 | APPROVED |
| REQ-AUTH-007 | SPEC-AUTH-001, SPEC-AUTH-006 | APPROVED |
| REQ-AUTH-008 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-009 | SPEC-AUTH-002 | APPROVED |
| REQ-AUTH-010 | SPEC-AUTH-003 | APPROVED |
| REQ-AUTH-011 | SPEC-AUTH-004 | APPROVED |
| REQ-AUTH-012 | SPEC-AUTH-007 | APPROVED |
| REQ-AUTH-013 | SPEC-AUTH-007 | APPROVED |
| REQ-AUTH-014 | SPEC-AUTH-005 | APPROVED |
| REQ-AUTH-015 | SPEC-AUTH-006 | APPROVED |
| REQ-AUTH-016 | SPEC-AUTH-009 | APPROVED |
| REQ-AUTH-017 | Todas (auditoría transversal) | APPROVED |
| REQ-AUTH-018 | SPEC-AUTH-001 | APPROVED |
| REQ-AUTH-019 | SPEC-AUTH-007 | APPROVED |
| REQ-AUTH-020 | SPEC-AUTH-002 | APPROVED |
| REQ-AUTH-021 | *(transversal — ver ADR-001, ADR-002)* | — |
| REQ-AUTH-022 | *(transversal — ver ADR-013)* | — |
| REQ-AUTH-023 | Todas (transversal) | APPROVED |
| REQ-AUTH-024 | Todas (transversal) | APPROVED |
| REQ-AUTH-025 | SPEC-AUTH-001, SPEC-AUTH-007 | APPROVED |
| REQ-AUTH-026 | *(pendiente — depende del esquema concreto a definir en implementación)* | — |
| REQ-AUTH-027 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-028 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-029 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-030 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-031 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-032 | SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-033 | SPEC-AUTH-009, SPEC-AUTH-010 | APPROVED |
| REQ-AUTH-034 | SPEC-AUTH-010 | APPROVED |

## 3. Specification → Dominio

| Specification | Concepto de dominio | Relación |
|---|---|---|
| SPEC-AUTH-001 | User, PasswordCredential, UserRoleAssignment, AdGroupRoleMapping | Autenticación, aprovisionamiento y sincronización |
| SPEC-AUTH-002 | RefreshToken, RefreshTokenFamily | Rotación y detección de reuse |
| SPEC-AUTH-003 | RefreshToken, RefreshTokenFamily | Revocación por logout |
| SPEC-AUTH-004 | RefreshToken, User | Revocación administrativa |
| SPEC-AUTH-005 | User, Role, Permission | Proyección de identidad/autorización |
| SPEC-AUTH-006 | User, Role, Permission, UserRoleAssignment | Decisión de autorización |
| SPEC-AUTH-007 | User, PasswordCredential, PasswordRecoveryToken | Gestión de contraseña LOCAL |
| SPEC-AUTH-008 | AdGroupRoleMapping, Role | Administración del mapping |
| SPEC-AUTH-009 | User, PasswordCredential | Bootstrap administrativo |
| SPEC-AUTH-010 | User, Role, Permission, UserRoleAssignment | Administración de identidad y autorización |

## 4. Specification → Estado de implementación y Pruebas

**Actualizado al cierre de la etapa "SPEC-AUTH-004/008 + Permission Catalog" (2026-09-07).** Conforme a `.claude/sdd/traceability.md §9` ("la trazabilidad no requiere registrar cada método") y §23 ("la matriz debe mantenerse simple"), esta tabla referencia las clases de prueba representativas de cada Specification, no cada uno de los ~316 métodos de test existentes bajo `com.IntraNet.Laucom.security.**`. No se usan identificadores `TEST-AUTH-NNN` individuales: dado el volumen real (muy superior a lo previsto al redactar este documento antes de la implementación), identificar cada test por un código separado duplicaría información ya presente — y con nombres más descriptivos — en el propio nombre de cada método de test (convención `nombreDelEscenario_condición_consecuencia`, ver ejemplos en cualquier `*Test.java` del módulo). La clase de test es la unidad de trazabilidad estable; el método de test documenta el escenario en su propio nombre.

| Specification | Estado de implementación | Pruebas representativas |
|---|---|---|
| SPEC-AUTH-001 | **IMPLEMENTADO** (Fases 4, 5, 12) | `domain.model.UserTest`, `application.authentication.{AuthenticateLocalUserUseCaseTest, AuthenticateActiveDirectoryUserUseCaseTest}`, `infrastructure.directory.ActiveDirectoryAttributeCodecTest`, `infrastructure.rest.AuthenticationControllerTest` (login), `infrastructure.persistence.UserAuthorizationPersistenceIT` (constraints, requiere Docker) |
| SPEC-AUTH-002 | **IMPLEMENTADO** (Fase 9) | `domain.model.RefreshTokenTest`, `application.session.{RenewSessionUseCaseTest, SessionIssuerTest}`, `infrastructure.token.{JwtTokenAdapterTest, JwtSigningKeysTest}`, `infrastructure.rest.AuthenticationControllerTest` (refresh) |
| SPEC-AUTH-003 | **IMPLEMENTADO** (Fase 11) | `application.session.{LogoutUseCaseTest, LogoutAllUseCaseTest}`, `infrastructure.rest.AuthenticationControllerTest` (logout/logout-all) |
| SPEC-AUTH-004 | **IMPLEMENTADO** (2026-09-07) | `application.admin.RevokeUserSessionsUseCaseTest`, `infrastructure.rest.UserAdminControllerTest` (`revokeSessions_*`) |
| SPEC-AUTH-005 | **IMPLEMENTADO** (Fase 16) | `application.identity.GetCurrentUserUseCaseTest`, `infrastructure.rest.CurrentUserControllerTest` |
| SPEC-AUTH-006 | **IMPLEMENTADO** (Fases 6, 17) | `application.authorization.{AuthorizationServiceTest, ImmediateAuthorizationEffectTest}` |
| SPEC-AUTH-007 | **IMPLEMENTADO** (Fase 3) | `application.password.{ChangePasswordUseCaseTest, RequestPasswordRecoveryUseCaseTest, ConfirmPasswordRecoveryUseCaseTest}`, `domain.model.PasswordRecoveryTokenTest`, `domain.service.PasswordPolicyTest`, `infrastructure.password.Argon2PasswordHasherAdapterTest`, `infrastructure.email.LoggingEmailSenderAdapterTest` (Fase 20: ausencia de secretos en logs) |
| SPEC-AUTH-008 | **IMPLEMENTADO** (2026-09-07) | `domain.model.AdGroupRoleMappingTest`, `application.admin.{CreateAdGroupMappingUseCaseTest, UpdateAdGroupMappingUseCaseTest, DeleteAdGroupMappingUseCaseTest, ListAdGroupMappingsUseCaseTest}`, `infrastructure.rest.AdGroupMappingAdminControllerTest` |
| SPEC-AUTH-009 | **IMPLEMENTADO** (Fase 12; RN-05 completado en Fase 20) | `application.admin.BootstrapMasterAdminUseCaseTest`, `application.authentication.AuthenticateLocalUserUseCaseTest` (auto-desbloqueo por cooldown de `MASTER_ADMIN`, RN-05) |
| SPEC-AUTH-010 | **IMPLEMENTADO** (Fases 17, 18, 20) | `application.admin.*UseCaseTest` (18 clases: alta/consulta/modificación/activación de Users, Roles, Permissions, asignación de roles), `infrastructure.rest.{UserAdminControllerTest, RoleAdminControllerTest, PermissionAdminControllerTest}`, `domain.model.{RoleTest, PermissionTest}` |

**Transversal** (REQ-AUTH-017, 021, 023, 024 — auditoría, arquitectura, secretos, correlationId): `architecture.SecurityModuleArchitectureTest` (REQ-AUTH-021, 8 reglas — incluye `adminUseCasesMustEnforceAuthorization`, Fase 22), `infrastructure.persistence.adapter.{DualWriteAuditAdapterTest, SecurityAuditEventPersistenceMapperTest, DatabaseRateLimiterAdapterTest}`, `infrastructure.web.{CorrelationIdFilterTest, JwtAuthenticationFilterTest, ProblemDetailAuthenticationEntryPointTest}`.

**Permission Catalog** (`ADR-021`, mecanismo transversal de reutilización, sin Specification propia — sirve a `SPEC-AUTH-010 §5` "administración de Permissions"): `application.permissioncatalog.SynchronizePermissionCatalogUseCaseTest`.

## 5. Specification → ADR

| Specification | ADR | Motivo |
|---|---|---|
| SPEC-AUTH-001 | ADR-002, ADR-003, ADR-004, ADR-005, ADR-017, ADR-018 | Identidad AD, shadow identity, mapping, fail closed, provider explícito |
| SPEC-AUTH-002 | ADR-007, ADR-008, ADR-009, ADR-010 | Estrategia de tokens y rotación |
| SPEC-AUTH-003 | ADR-007, ADR-010 | Transporte y ciclo de vida del Refresh Token |
| SPEC-AUTH-004 | — | Sin ADR propio; consume la decisión de ADR-006 vía SPEC-AUTH-006 |
| SPEC-AUTH-005 | — | Sin ADR propio |
| SPEC-AUTH-006 | ADR-006, ADR-015 | Fuente de verdad y mecanismo de evaluación |
| SPEC-AUTH-007 | ADR-011 | Hashing de contraseñas |
| SPEC-AUTH-008 | ADR-005 | Mapping AD Group → Role |
| SPEC-AUTH-010 (Permission Catalog) | ADR-021 | Reutilización del módulo: declaración de permisos de aplicación (nueva, 2026-09-07) |
| SPEC-AUTH-009 | ADR-011, ADR-019 | Hashing de la credencial de bootstrap; continuidad administrativa |
| SPEC-AUTH-010 | ADR-006, ADR-011, ADR-019, ADR-020 | Autorización viva, hashing, continuidad de MASTER_ADMIN, soft deactivation |

## 6. API Contract

La columna **Estado** refleja la aprobación del *diseño* del contrato (fecha de la Specification), no necesariamente su presencia en el código — ver la columna **Implementado** (actualizada al cierre de Fase 20) para el estado real.

| Specification | API Contract | Estado (diseño) | Implementado |
|---|---|---|---|
| SPEC-AUTH-001 | `POST /auth/login`, con `provider` explícito | APPROVED | Sí (Fase 10) |
| SPEC-AUTH-002 | `POST /auth/refresh` | APPROVED | Sí (Fase 10) |
| SPEC-AUTH-003 | `POST /auth/logout`, `POST /auth/logout/all` | APPROVED | Sí (Fase 11) |
| SPEC-AUTH-004 | `POST /auth/admin/users/{userId}/revoke-sessions` | APPROVED | Sí (2026-09-07) |
| SPEC-AUTH-005 | `GET /auth/me`, incluye `email` nullable | APPROVED | Sí (Fase 16) |
| SPEC-AUTH-006 | No aplica (mecanismo transversal, sin endpoint propio) | APPROVED | Sí (mecanismo, Fases 6/17) |
| SPEC-AUTH-007 | `POST /auth/password/change`, `/recovery/request`, `/recovery/confirm` | APPROVED | Sí (Fase 3) |
| SPEC-AUTH-008 | `/auth/admin/ad-group-mappings` | APPROVED | Sí (2026-09-07) |
| SPEC-AUTH-009 | No aplica (proceso de arranque, no endpoint HTTP) | APPROVED | Sí (Fase 12, RN-05 completado Fase 20) |
| SPEC-AUTH-010 | `/auth/admin/users`, `/auth/admin/roles`, `/auth/admin/permissions`, `/auth/admin/users/{userId}/roles` | APPROVED | Sí (Fases 17, 18, 20) |

## 7. GAPs — cierre documental 2026-09-05

Todos los GAPs abiertos fueron resueltos mediante el Decision Ledger del cierre documental del 2026-09-05 y ya están incorporados en las Specifications y ADRs correspondientes. Registro para historial (ver `.claude/sdd/change-management.md §22`, preservación de historial):

| GAP | Specification | Resolución | Incorporado en |
|---|---|---|---|
| GAP-AUTH-001 | SPEC-AUTH-001 | Fail closed: sin tokens ni fallback a roles previos ante fallo de sincronización de grupos AD. | RN-09, `ADR-017` |
| GAP-AUTH-002 | SPEC-AUTH-001 | `provider` explícito y obligatorio en el request; sin autodetección por username. | RN-08, `ADR-018` |
| GAP-AUTH-003 | SPEC-AUTH-002 | Sin grace period; refresh concurrente activa la detección de reuse estándar. | RN-05, `ADR-008` (actualizado) |
| GAP-AUTH-004 | SPEC-AUTH-005 | `email` incluido en `GET /auth/me`, nullable, sin valor de autorización. | §12 de la SPEC |
| GAP-AUTH-005 | SPEC-AUTH-007 | Nueva solicitud de recuperación invalida tokens de recuperación pendientes previos. | RN-06 |
| GAP-AUTH-006 | SPEC-AUTH-009 | Fail-fast en el arranque si no hay administrador ni secreto de bootstrap. | RN-07, `ADR-019` |
| GAP-AUTH-007 | SPEC-AUTH-009 | Protección transaccional y bloqueante del último administrador local; preferencia por soft-deletion. | RN-08, `ADR-019` |
| GAP-AUTH-008 | SPEC-AUTH-009 | Hallazgo de Fase 20 (Testing): RN-05 (cooldown nunca permanente para `MASTER_ADMIN`) no estaba implementado (`User.isCooldownElapsed()` existía sin ningún llamador). Resuelto: auto-desbloqueo con cooldown de duración fija (no creciente) exclusivo para `MASTER_ADMIN`, vía `AuthenticateLocalUserUseCase`. Lo "creciente" del texto original de RN-05 queda fuera de alcance de V1. | RN-05 (corregida), `SPEC-AUTH-009 §15` |

**Estado:** todas las Specifications de este módulo (`SPEC-AUTH-001` a `SPEC-AUTH-010`) están en estado `APPROVED`, sin GAPs abiertos a nivel documental. Para el estado de **implementación** real (código + pruebas), ver §4 y §10 — las 10 Specifications tienen código y pruebas desde el 2026-09-07.

## 8. Blocker de administración RBAC — CERRADO (2026-09-06)

**Histórico:** durante el cierre documental del 2026-09-05 se detectó que no existía una Specification para la administración de Usuarios, Roles y Permisos, necesaria para materializar `UserRoleAssignment(provenance=GRANTED_EXPLICITLY)` (ya modelado en `02-domain/entities.md`, requerido por `REQ-AUTH-007`).

**Resolución:** este blocker fue cerrado mediante el mini-ciclo de diseño del 2026-09-06, que produjo `SPEC-AUTH-010` (Administración de Usuarios, Roles y Permisos), `ADR-020` (soft deactivation), el addendum a `ADR-019` (formalización del rol `MASTER_ADMIN`), y las invariantes `INV-AUTH-013`, `INV-AUTH-014`, `INV-AUTH-015`. No quedan blockers funcionales conocidos relacionados con administración RBAC.

## 9. Reglas de trazabilidad aplicadas
- El API Contract no constituye un nuevo eslabón de la cadena principal de trazabilidad; es una relación opcional de la Specification (junto con el UC), según `.claude/sdd/traceability.md §3` (ya actualizado) y `.claude/documentation/conventions.md §7`.
- No se utilizan identificadores `TEST-AUTH-NNN` individuales (ver §4): el volumen real de pruebas hizo que ese esquema dejara de aportar valor frente a referenciar directamente la clase de test, cuyo nombre de método ya documenta el escenario.

## 10. Estado de implementación — cierre de Fase 22 y SPEC-AUTH-004/008 (2026-09-07)

Este documento fue redactado antes de comenzar la implementación (cierre documental 2026-09-05/06). Tras completar las 22 fases del plan de implementación y, a continuación, cerrar los dos gaps que quedaban abiertos, el estado real es:

- **10 de 10 Specifications completamente implementadas y probadas**: SPEC-AUTH-001 a SPEC-AUTH-010.
  - **SPEC-AUTH-004** (revocación administrativa de sesión, 2026-09-07): `RevokeUserSessionsUseCase` + `POST /auth/admin/users/{userId}/revoke-sessions` (`UserAdminController`), permiso `SESSION_REVOKE_ANY` (migración V9), auditoría `ADMIN_SESSION_REVOCATION`.
  - **SPEC-AUTH-008** (administración del mapping AD Group → Role, 2026-09-07): `{Create,Update,Delete,List}AdGroupMappingUseCase` + `AdGroupMappingAdminController` (`/auth/admin/ad-group-mappings`), permiso `AD_MAPPING_MANAGE` (ya existente desde Fase 12), auditoría `AD_MAPPING_CHANGED`.
- **Permission Catalog** (`ADR-021`, 2026-09-07): mecanismo de reutilización que permite a una aplicación consumidora declarar sus propios permisos (`PermissionCatalog`/`PermissionDescriptor`) sin tocar el código del módulo Security/Auth; el propio módulo declara los suyos por el mismo mecanismo (`SecurityModulePermissionCatalogConfig`).
- **316 pruebas** bajo `com.IntraNet.Laucom.security.**` (excluyendo la IT de Testcontainers, no ejecutable en este sandbox sin Docker), 0 fallos.
- **ArchUnit**: 8/8 reglas verificadas (dominio puro; `application` no depende de `infrastructure`; `RoleJpaRepository`/`PermissionJpaRepository` sin borrado físico; todo Use Case administrativo — salvo `BootstrapMasterAdminUseCase` — invoca `AdminActionAuthorizer`, regla agregada en Fase 22).
- **Hallazgos cerrados**: RN-05 SPEC-AUTH-009 (Fase 20/22, ver GAP-AUTH-008 §7); `Path` del cookie de Refresh Token (Fase 22, ver `ADR-010`).

No quedan Specifications de este módulo sin implementación. Limitaciones conocidas restantes: Testcontainers/Docker no disponible en este sandbox (una IT de persistencia no se ejecuta aquí); una excepción `IllegalArgumentException`/`MethodArgumentTypeMismatchException` por un identificador UUID malformado en un path variable o en el cuerpo de un request administrativo cae en el manejador catch-all genérico (500) en vez de un 400 dedicado — patrón preexistente en todos los controladores administrativos desde la Fase 17, no introducido ni corregido en este cierre por exceder su alcance.
