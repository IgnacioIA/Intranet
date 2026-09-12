# Arquitectura — Módulo de Seguridad (AUTH)

## 1. Estilo arquitectónico

DDD + Hexagonal Architecture (Ports & Adapters). El dominio del módulo no depende de Spring Security, JPA/Hibernate, ninguna librería JWT concreta, ni de una implementación concreta de directorio (Active Directory/LDAP) — ver `ADR-001`, `ADR-002` y REQ-AUTH-021.

Justificación de la separación (no es abstracción "por arquitectura"): ya existen, desde V1, al menos dos implementaciones reales de un mismo Port (`IdentityDirectoryPort` → adapter de Active Directory en producción, adapter Fake en tests), lo cual por sí solo justifica la frontera sin necesidad de imaginar reutilización futura.

```text
                         ┌───────────────────────────┐
                         │         Frontend           │
                         │  (mismo sitio que backend)  │
                         └─────────────┬───────────────┘
                                       │ HTTPS · REST · RFC 7807
                                       ▼
        ┌───────────────────────────────────────────────────────────┐
        │                    REST Layer (Adapter IN)                 │
        │  Controllers · DTOs request/response · Bean Validation     │
        │  Mapea DTO ↔ Command/Query de Application                  │
        │  (Spring Security SecurityFilterChain envuelve esta capa)  │
        └────────────────────────────┬─────────────────────────────┘
                                      ▼
        ┌───────────────────────────────────────────────────────────┐
        │                     Application Layer                      │
        │  Use Cases (ver SPEC-AUTH-* — UC-AUTH-*):                   │
        │   AuthenticateLocalUser · AuthenticateADUser                │
        │   ProvisionADUser · SynchronizeADRoles                      │
        │   RefreshAuthentication · Logout · LogoutAll                │
        │   RevokeUserSessions · GetCurrentUser · AuthorizeRequest    │
        │   ChangePassword · RequestPasswordRecovery                  │
        │   ConfirmPasswordRecovery · ManageAdGroupMapping             │
        │   BootstrapMasterAdmin                                       │
        │  Orquesta Ports, publica Application/Domain Events           │
        └───┬──────────────┬───────────────┬───────────────┬────────┘
            ▼              ▼               ▼               ▼
        ┌────────┐   ┌───────────┐   ┌───────────┐   ┌─────────────┐
        │ Domain │◄──┤   Ports    ├──►│   Ports   │   │    Ports    │
        │ Model  │   │   (out)    │   │   (out)   │   │    (out)    │
        └────────┘   └─────┬─────┘   └─────┬─────┘   └──────┬──────┘
                            ▼               ▼                ▼
              ┌─────────────────────────────────────────────────────┐
              │                     Infrastructure                   │
              │  IdentityDirectoryPort → ActiveDirectoryAdapter (LDAP)│
              │                        → FakeDirectoryAdapter (tests) │
              │  UserRepositoryPort    → JPA/MySQL Adapter            │
              │  TokenPort             → JWT Adapter (firma/parseo)   │
              │  AuditPort             → DB Adapter + log estructurado│
              │  PasswordHasherPort    → Argon2id Adapter             │
              │  RateLimiterPort       → DB Adapter (Redis futuro)    │
              └───────────────────────────────────────────────────────┘
```

## 2. Límites de módulos

El módulo se organiza, dentro del proyecto actual, en un paquete propio (`security`/`auth`, nombre definitivo a resolver durante la implementación) con subpaquetes `domain`, `application`, `infrastructure`. No se extrae como artefacto Maven independiente en V1 (ver `ADR-016`).

## 3. Ubicación de Spring Security

Spring Security **no pertenece al dominio ni a la capa de aplicación**. Se integra exclusivamente en el borde:

- `SecurityFilterChain` y sus filtros (incluyendo un `CorrelationIdFilter`) resuelven autenticación a nivel HTTP y delegan la verificación real a los Use Cases de Application.
- Un `AuthenticationProvider` custom traduce el resultado de `AuthenticateLocalUser`/`AuthenticateADUser` a un `Authentication`/`SecurityContext` de Spring.
- `GrantedAuthority` se deriva, en el borde, de los `Permission` vigentes del `User` (nunca al revés: el dominio no conoce `GrantedAuthority`).

Ver `ADR-014`.

## 4. Mecanismos para cross-cutting concerns

Se descartó explícitamente el Decorator Pattern (hipótesis original) por no aportar separación adicional frente a mecanismos ya idiomáticos del ecosistema:

| Concern | Mecanismo elegido | Motivo |
|---|---|---|
| Autenticación | Spring Security `SecurityFilterChain` + `AuthenticationProvider` | Mecanismo estándar, ya resuelve el problema sin capas adicionales. |
| Autorización por permiso | Servicio de aplicación explícito (`AuthorizeRequest`, SPEC-AUTH-006), invocado desde el borde (ej. `@PreAuthorize` delegando a este servicio, o un filtro dedicado) | Decisión explícita y testeable, no dispersa en anotaciones sin lógica propia. |
| Correlation ID | Servlet `Filter` dedicado | Concern por-request, no por-método; un `Filter` es más simple e idiomático que AOP para esto. |
| Auditoría | Llamada directa a `AuditPort` desde cada Use Case (corregido en ADR-012 §4, Fase 14: el mecanismo de Events + listener originalmente previsto nunca se implementó así) | El propio Use Case ya sabe qué ocurrió y con qué resultado; no se identificó un caso real que requiera múltiples consumidores independientes del mismo evento. |
| Logging técnico | Configuración estándar del framework de logging (no forma parte del dominio) | No requiere patrón de diseño propio. |

Ver `ADR-015`.

## 5. Principios arquitectónicos específicos de este módulo

- El dominio nunca es la fuente de autorización cacheada: toda decisión de autorización se resuelve contra el estado vigente (ver `docs/03-architecture/security.md` y REQ-AUTH-015).
- Ningún Port se diseña con más de una implementación especulativa en V1 (evitar sobreingeniería: un solo adapter real + un fake de test por Port).
- La extracción a artefacto reutilizable (JAR, eventual Starter) se pospone hasta que exista un segundo consumidor real (`ADR-016`).

## 6. Reutilización — Permission Catalog (`ADR-021`)

El módulo Security/Auth es dueño de sus propios permisos (los ya existentes: `USER_READ`, `USER_MANAGE`, `ROLE_READ`, `ROLE_MANAGE`, `PERMISSION_READ`, `PERMISSION_MANAGE`, `ROLE_ASSIGN`, `ROLE_REVOKE`, `SESSION_REVOKE_ANY`, `AD_MAPPING_MANAGE`, `VIEW_ONBOARDING_INFO` — ver `WellKnownPermissions`). Una aplicación consumidora (ej. una futura Intranet) es dueña de su propio namespace de permisos (ej. `INTRANET.DOCUMENT_READ`); el módulo no conoce ni necesita conocer su significado.

```text
                  ┌──────────────────────┐
                  │   Security/Auth V1    │
                  │  USER_*, ROLE_*, ...  │
                  │  RBAC · AuthN/AuthZ   │
                  │  AD mapping           │
                  └──────────┬───────────┘
                             │
                     Permission Catalog
                             │
             ┌───────────────┴───────────────┐
             │                               │
      ┌──────▼───────┐                ┌──────▼───────┐
      │   Intranet   │                │   Otra app   │
      │  INTRANET.*  │                │    CRM.*     │
      └──────────────┘                └──────────────┘
```

Cada aplicación (el propio módulo incluido) declara un bean `PermissionCatalog` (`application.permissioncatalog`); al arrancar, `SynchronizePermissionCatalogUseCase` crea únicamente los permisos declarados que todavía no existen en DB (`active = true`), sin tocar jamás los ya existentes (ni su `active`, ni su `description` — la decisión administrativa de desactivar un permiso es permanente frente a redeploys). Agregar un permiso de aplicación nuevo requiere solo agregar un descriptor en la aplicación consumidora, nunca tocar el módulo Security/Auth. Detalle completo, alternativas consideradas y reglas exactas en `ADR-021`.

## 7. Documentación de API generada desde el código

El contrato HTTP máquina-legible (OpenAPI 3) no se mantiene a mano: se genera en cada arranque a partir de los `@RestController` reales (springdoc-openapi), y se expone en `/v3/api-docs` y `/swagger-ui.html`. Un archivo estático versionado (`docs/07-api-contract/openapi.json`) se regenera con `mvn verify -Pgenerate-openapi` para quien necesite importarlo sin correr la aplicación. Las Specifications (`docs/06-specifications/`) siguen siendo la fuente de verdad de *por qué* existe cada endpoint y sus reglas de negocio; el OpenAPI generado es la forma exacta de *cómo* invocarlo. Ver `docs/07-api-contract/README.md` para el flujo completo.
