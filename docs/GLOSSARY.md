# Glosario del Proyecto

## 1. Propósito

Este documento registra el vocabulario oficial de Laucom: los términos de negocio, dominio y sistema que pueden generar ambigüedad, y los códigos de contexto utilizados en los identificadores definidos por `.claude/documentation/conventions.md`.

No contiene reglas de negocio completas — solo definiciones y el registro de contextos válidos.

---

## 2. Contextos registrados

| Contexto | Significado | Módulo/capacidad |
|---|---|---|
| `AUTH` | Identidad, autenticación, autorización y auditoría de seguridad | Módulo de Seguridad (identidad LOCAL/Active Directory, sesiones, RBAC, auditoría) |

Cualquier identificador (`REQ-`, `SPEC-`, `UC-`, `TEST-`) que utilice un contexto no listado en esta tabla debe considerarse un GAP y resolverse antes de crear el artefacto correspondiente.

---

## 3. Términos

| Término | Definición |
|---|---|
| **Identity Provider (Provider)** | Origen de la identidad de un usuario dentro del sistema. V1 admite `LOCAL` (credenciales gestionadas por la propia aplicación) y `ACTIVE_DIRECTORY` (credenciales gestionadas por Active Directory). |
| **Shadow Identity** | Representación local, dentro de la aplicación, de un usuario cuya autenticación es responsabilidad de un proveedor externo (Active Directory). No duplica la contraseña ni todos los atributos del proveedor externo. |
| **external_id** | Identificador estable e inmutable que relaciona una Shadow Identity con su cuenta en el proveedor externo. Para Active Directory es el `objectGUID`. |
| **Rol derivado (`DERIVED_FROM_AD`)** | Rol asignado a un usuario como resultado de la evaluación de sus grupos de Active Directory contra el mapping vigente. Se recalcula en cada inicio de sesión exitoso contra AD. |
| **Rol explícito (`GRANTED_EXPLICITLY`)** | Rol asignado directamente por un administrador de la aplicación, independiente de los grupos de Active Directory. No se modifica por la sincronización de AD. |
| **AD Group → Role Mapping** | Configuración, almacenada en base de datos, que asocia un grupo de Active Directory con un rol de la aplicación. Es la única vía por la cual la pertenencia a un grupo AD puede traducirse en autorización dentro de la aplicación. |
| **Onboarding** | Estado y conjunto mínimo de acceso otorgado a un usuario que todavía no posee ningún rol válido dentro de la aplicación. |
| **Master Admin / Break-glass Admin** | Cuenta administrativa local, independiente de Active Directory, necesaria para operar la aplicación aunque Active Directory no esté disponible. Formalizada como el rol de sistema `MASTER_ADMIN` (`SPEC-AUTH-010`), cuya continuidad está garantizada por `INV-AUTH-013`. |
| **Soft deactivation** | Ciclo de vida administrativo de `Role`/`Permission` mediante un atributo `active`, sin eliminación física, para preservar la integridad histórica de `Security Audit` y de las asignaciones existentes (`ADR-020`). |
| **Access Token** | Credencial de corta duración (JWT) utilizada para autenticar solicitudes. No es la fuente de verdad de autorización. |
| **Refresh Token** | Credencial opaca de mayor duración utilizada exclusivamente para obtener un nuevo Access Token, sujeta a rotación. |
| **Refresh Token Family** | Conjunto de Refresh Tokens vinculados por sucesivas rotaciones, utilizado para detectar reutilización de un token ya revocado. |
| **Security Audit** | Registro de eventos semánticos relacionados con seguridad (login, logout, revocación, cambios de permisos), distinto de los logs técnicos de la aplicación. |
| **Application Logs** | Registro técnico orientado a diagnóstico, depuración y operación, sin valor de auditoría formal. |
| **Correlation ID** | Identificador único de una solicitud, propagado a través de logs y eventos de auditoría para permitir su reconstrucción end-to-end. |

---

## 4. Ports del módulo

| Port | Responsabilidad |
|---|---|
| **IdentityDirectoryPort** | Abstrae el directorio de identidad externo (verificación de credenciales, identidad, grupos). Implementación V1: adapter de Active Directory (LDAP) y un adapter Fake para tests. Nombrado de forma abstracta a propósito (no `ActiveDirectoryPort`), ver `ADR-002`. |
| **UserRepositoryPort** | Persistencia de `User` y sus asociaciones (roles, credenciales). |
| **TokenPort** | Firma/parseo del Access Token (JWT) y gestión del ciclo de vida del Refresh Token. |
| **PasswordHasherPort** | Hashing y verificación de contraseñas `LOCAL` (Argon2id, `ADR-011`). |
| **AuditPort** | Registro de `SecurityAuditEvent` (doble escritura DB + log estructurado, `ADR-012`). El dominio no depende de este port directamente; lo consume la capa de aplicación a través de eventos. |
| **RateLimiterPort** | Conteo de intentos de autenticación (IP + identidad). Implementación V1: base de datos; preparado para un adapter distribuido (Redis) sin implementarlo (`ADR-013`). |

---

## 5. Reglas de evolución

Un nuevo contexto o término debe agregarse a este glosario antes de utilizarse en un identificador o en una Specification. No debe inventarse vocabulario ad-hoc dentro de un artefacto individual.
