# Estrategia de Testing — Módulo de Seguridad (AUTH)

Nota de ubicación: este archivo no estaba contemplado en `.claude/documentation/structure.md §7`. Se agrega como archivo nuevo dentro de la carpeta ya existente `03-architecture/` (sin crear una carpeta nueva), por ser contenido estrechamente ligado a las decisiones arquitectónicas del módulo (en particular, las reglas de ArchUnit). Las reglas generales de testing del proyecto (no específicas de este módulo) pertenecen a `.claude/development/testing.md`.

## 1. Niveles de prueba

| Nivel | Alcance | Herramientas |
|---|---|---|
| Unit | Dominio puro: transiciones de `User`, rotación/family de `RefreshToken`, evaluación de `Permission`. Sin contexto Spring. | JUnit |
| Application | Use Cases con Ports mockeados (Fake AD, repositorio en memoria). | JUnit + mocks |
| Persistence | Mapeo JPA, constraints de unicidad (INV-AUTH-001, INV-AUTH-002), migraciones Flyway aplicadas desde cero. **No se asume que H2 (u otra base en memoria) sea equivalente a MySQL** para este módulo: las pruebas de persistencia corren contra MySQL real. | Testcontainers (MySQL) |
| API | Contrato HTTP: status codes, forma RFC 7807, headers, cookies. | MockMvc / RestAssured |
| Security | Escenarios de abuso y de negativa (ver §3). | Igual que API, con foco en negativos |
| Architecture | El paquete `domain` no depende de Spring, JPA, librerías JWT ni de directorio. | ArchUnit |
| AD Integration | Contra un `FakeDirectoryAdapter` en memoria para el 100% de CI. | JUnit |

## 2. Reglas de ArchUnit mínimas (REQ-AUTH-021)

- `domain` no importa `org.springframework..`
- `domain` no importa `jakarta.persistence..` / `javax.persistence..`
- `domain` no importa ninguna librería JWT concreta.
- `domain` no importa clases de acceso a directorio (LDAP/AD).
- `application` no importa clases de `infrastructure`.

## 3. Escenarios de seguridad a cubrir (mínimo)

Login: LOCAL exitoso · AD exitoso · provider ausente/inválido en el request (400) · credenciales incorrectas (LOCAL y AD, respuesta equivalente) · usuario inexistente (respuesta equivalente a credencial incorrecta) · usuario `DISABLED` · usuario `LOCKED` · AD inalcanzable (`AD_CONNECTION_FAILURE`, distinguible de credencial incorrecta) · **fallo cerrado ante error de obtención de grupos AD tras credenciales correctas** (`AD_LOOKUP_FAILURE`, sin emisión de tokens, sin fallback a roles previos) · usuario AD nuevo sin grupos mapeados → `PENDING_ONBOARDING` · usuario AD nuevo con grupos mapeados → `ACTIVE` · usuario con grupos mixtos (mapeados y no mapeados) · usuario con múltiples roles · rol explícito que sobrevive a una resincronización AD que ya no lo deriva.

Autorización: permiso concedido · permiso denegado · cuenta no `ACTIVE` con token criptográficamente válido (debe denegarse) · cambio de permisos/roles con efecto inmediato en la siguiente solicitud (sin esperar expiración del token).

Tokens: Access Token expirado · Refresh Token expirado · Refresh Token revocado · Refresh Token reutilizado (familia completa revocada) · **refresh concurrente sin grace period** (dos requests simultáneos con el mismo token — solo uno prospera, el otro dispara la misma detección de reuse que una reutilización maliciosa).

Sesión: logout individual · logout-all · revocación administrativa (incluye verificación de auditoría con actor y sujeto, y que se audita tanto `AUTHORIZATION_GRANTED` como `AUTHORIZATION_DENIED` por ser operación sensible).

Contraseña: cambio autenticado exitoso · recuperación solicitada (cuenta existente e inexistente → respuesta equivalente) · confirmación con token válido, expirado y ya usado · **nueva solicitud de recuperación invalida el token de recuperación pendiente anterior** · recuperación/cambio exitosos revocan todas las sesiones activas · usuario AD solicitando cambio/recuperación → rechazo orientativo sin revelar detalles internos.

Master Admin: bootstrap exitoso en primer arranque · arranque idempotente si ya existe administrador · **fail-fast si no existe administrador ni secreto de bootstrap configurado** · cambio de contraseña obligatorio en el primer uso y **la credencial de bootstrap deja de ser válida** tras ese cambio · login del Master Admin funcional con Active Directory completamente caído (no depende de `IdentityDirectoryPort`) · protección transaccional contra deshabilitar/eliminar la última cuenta administrativa activa · cooldown no permanente ante intentos fallidos sobre esta cuenta.

Otros: rate limit excedido (login) · ausencia de secretos en logs/auditoría (inspección de serializers) · propagación de `correlationId` end-to-end.

Administración (Users/Roles/Permissions — `SPEC-AUTH-010`):
- Usuarios: admin consulta usuario · admin desactiva usuario (revoca sesiones, audita `USER_DISABLED`) · usuario desactivado no puede acceder · admin intenta desactivar/bloquear/deprovisionar al último `MASTER_ADMIN` → rechazado, auditado como `ADMIN_OPERATION_DENIED` · alta de usuario LOCAL con username duplicado → 409 · intento de editar identidad de un usuario AD → rechazado.
- Roles: crear Role · modificar Role (permisos) · desactivar Role (pierde efecto inmediato sin borrar asignaciones) · desactivar Role de sistema → rechazado · Role inexistente → 404 · Role duplicado → 409.
- Permissions: crear Permission · modificar descripción · intento de renombrar → rechazado · desactivar Permission de sistema (`VIEW_ONBOARDING_INFO`) → rechazado · Permission inexistente → 404 · Permission duplicada → 409.
- Autorización inmediata: Role agregado → siguiente request autorizada sin nuevo token · Role revocado → siguiente request denegada sin esperar expiración · Permission agregada/revocada → mismo efecto inmediato.
- Coexistencia con AD: rol explícito sobrevive a sincronización AD · asignación explícita sobre un rol ya derivado hace *upgrade* de provenance sin duplicar fila · revocar explícitamente una asignación puramente `DERIVED_FROM_AD` → rechazado, orientando a `SPEC-AUTH-008` · rol derivado reaparece en el siguiente login AD tras revocar la asignación explícita.
- Master Admin: protección transaccional del último Master Admin ante disable/lock/deprovision/revocación de rol/desactivación del rol `MASTER_ADMIN` · intento concurrente de remover al último Master Admin por dos vías simultáneas → el invariante `INV-AUTH-013` nunca queda violado al finalizar.

Administración de mapping AD Group → Role (`SPEC-AUTH-008`, implementado 2026-09-07): alta de mapping · alta duplicada (mismo grupo) → 409 · modificación de rol de un mapping existente · modificación del identificador de grupo hacia uno ya usado por otro mapping → 409 · modificación hacia un Role inexistente → 404 · modificación/alta hacia un Role inactivo → rechazado · baja → 204 · baja de mapping inexistente → 404 · baja no recorre usuarios existentes (efecto diferido a la siguiente sincronización AD, RN-04) · toda alta/baja/modificación audita `AD_MAPPING_CHANGED` con actor y timestamp · sin permiso `AD_MAPPING_MANAGE` → 403.

Revocación administrativa de sesiones (`SPEC-AUTH-004`, implementado 2026-09-07): admin autorizado revoca N familias activas → `revokedSessions = N` · segunda ejecución sin sesiones nuevas → `revokedSessions = 0`, sigue siendo 200 · usuario sin sesiones activas → 200, `revokedSessions = 0` · sin permiso `SESSION_REVOKE_ANY` → 403, audita `AUTHORIZATION_DENIED` · usuario objetivo inexistente → 404 · un Refresh Token previamente válido del usuario objetivo deja de poder usarse tras la revocación · los Access Tokens ya emitidos no se invalidan (expiran naturalmente) · audita `ADMIN_SESSION_REVOCATION` con actor y sujeto · admin revocando sus propias sesiones → no se bloquea, mismo resultado que el autoservicio.

Permission Catalog (`ADR-021`, mecanismo transversal, implementado 2026-09-07): permiso declarado ausente en DB → se crea `active = true` · permiso declarado ya existente y activo → permanece sin cambios · permiso declarado ya existente y desactivado por un administrador → permanece desactivado (la sincronización nunca reactiva) · sincronizar el mismo descriptor varias veces → nunca crea un duplicado · descriptor `systemPermission = true` → persiste como tal · descriptor `systemPermission = false` → persiste como tal · múltiples `PermissionCatalog` en el contexto → todos se sincronizan.

## 4. Principio general

Una prueba que solo verifica "el código compila y no lanza excepción" no se considera evidencia de cumplimiento de una SPEC. Toda prueba de seguridad debe poder relacionarse con un `TEST-AUTH-NNN` trazable a la SPEC y al criterio de aceptación que verifica (ver `05-traceability/traceability.md`).
