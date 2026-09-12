# ADR-002 — Active Directory como adapter opcional de identidad

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
El módulo debe soportar autenticación contra Active Directory, pero un proyecto futuro podría no necesitar AD en absoluto. Es necesario decidir cómo evitar que el dominio (o siquiera la capa de aplicación) quede acoplado a una implementación concreta de directorio.

## 4. Decisión
Se define un Port abstracto `IdentityDirectoryPort` (no `ActiveDirectoryPort`) que expone únicamente las operaciones necesarias: verificar credenciales, obtener identidad (incluyendo `objectGUID`) y obtener grupos del usuario. Active Directory se implementa como un adapter de infraestructura (`ActiveDirectoryAdapter`, vía LDAP) que implementa ese Port. Un proyecto sin AD simplemente no configura este adapter.

## 5. Alternativas consideradas

### Alternativa A — Nombrar el Port explícitamente `ActiveDirectoryPort`
**Ventajas:** más explícito sobre la tecnología actual.
**Desventajas:** cierra la puerta, por nombre, a un futuro adapter LDAP genérico o de otro proveedor de identidad; sugiere acoplamiento donde no debería haberlo.

### Alternativa B — `IdentityDirectoryPort` abstracto (elegida)
**Ventajas:** el nombre no compromete la tecnología; el dominio permanece agnóstico.
**Desventajas:** ninguna relevante — no implica trabajo adicional, es una decisión de nomenclatura.

## 6. Consecuencias

### Positivas
- Reutilización futura sin renombrar contratos existentes.

### Negativas
Ninguna significativa.

### Riesgos
- Ninguno relevante identificado.

## 7. Áreas afectadas
Application (definición del Port), Infrastructure (`ActiveDirectoryAdapter` y `NoOpIdentityDirectoryAdapter`, ver §9).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-002, REQ-AUTH-021
- Specifications: SPEC-AUTH-001
- ADR relacionados: ADR-001

## 9. Nota de implementación (pendiente de aprobación)

**Esta sección documenta un comportamiento ya implementado en el código. No cambia el Estado
(§1) de este ADR — sigue "Propuesto" — porque aceptarlo formalmente requiere una decisión
explícita del responsable del proyecto, no de Claude.**

Al escribir este ADR, "un proyecto sin AD simplemente no configura este adapter" (§4) quedó sin
resolver un detalle: `AuthenticateActiveDirectoryUserUseCase` y `AuthenticationController`
declaran `IdentityDirectoryPort` como dependencia obligatoria de constructor, así que con
`ActiveDirectoryAdapter` ausente (`ad.enabled=false`, su default) Spring no podía construir el
contexto en absoluto — un `UnsatisfiedDependencyException` en el arranque, no un comportamiento
en runtime.

Se agregó `NoOpIdentityDirectoryAdapter` (`security/infrastructure/directory/`), activo
exactamente cuando `ActiveDirectoryAdapter` no lo está
(`@ConditionalOnProperty(name="ad.enabled", havingValue="false", matchIfMissing=true)`, mutuamente
excluyente con `havingValue="true"` de `ActiveDirectoryAdapter`). Devuelve siempre
`IdentityDirectoryPort.DirectoryUnavailable` — el mismo resultado de dominio que ya existía para
"AD inalcanzable en runtime" (UC-AUTH-002 flujo 2b, `AD_CONNECTION_FAILURE`). No se introdujo
ningún concepto de dominio nuevo: se determinó que, desde la perspectiva del Use Case y del
cliente HTTP, "AD no configurado" y "AD temporalmente inalcanzable" son indistinguibles y les
corresponde la misma respuesta (503 `ad-unavailable`), consistente con RN-06 (SPEC-AUTH-001): la
disponibilidad del servicio no es información de la cuenta.

**Pendiente de decisión del responsable del proyecto:** si este comportamiento se considera
correcto y suficiente, corresponde pasar el Estado de este ADR a "Aceptado".
