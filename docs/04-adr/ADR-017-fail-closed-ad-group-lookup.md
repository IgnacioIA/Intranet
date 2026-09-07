# ADR-017 — Fail-closed ante fallo de sincronización de grupos AD durante login

## 1. Estado
Aceptado

## 2. Fecha
2026-09-05

## 3. Contexto
Un usuario de Active Directory puede autenticarse correctamente (AD valida sus credenciales) y, sin embargo, la consulta posterior de sus grupos puede fallar por un problema puntual de red, del propio directorio o de permisos de la cuenta de servicio utilizada para la consulta. Es necesario decidir qué ocurre con la sesión en ese escenario: la aplicación ya sabe que la persona es quien dice ser, pero no puede determinar con certeza qué roles le corresponden.

## 4. Decisión
El sistema falla cerrado (*fail closed*): si las credenciales AD son correctas pero falla la obtención o evaluación de los grupos del usuario, no se emiten tokens. La solicitud de login se rechaza con una condición de servicio (HTTP 503), distinguible de una credencial incorrecta, y se audita como `AD_LOOKUP_FAILURE`. No se emite una sesión con los roles previamente persistidos como sustituto.

## 5. Alternativas consideradas

### Alternativa A — Fail open (proceder con los roles previamente persistidos)
**Ventajas:** no interrumpe el acceso del usuario ante una falla transitoria y aislada del subsistema de consulta de grupos.
**Desventajas:** si el fallo de consulta de grupos coincide con un cambio real de privilegios (ej. el usuario fue removido de un grupo crítico y el fallo impide detectarlo), el usuario opera con una autorización potencialmente desactualizada sin que el sistema lo sepa. Contradice el principio de mínimo privilegio ante incertidumbre.

### Alternativa B — Fail closed (elegida)
**Ventajas:** ninguna sesión se emite jamás con una autorización que el sistema no pudo verificar en el momento del login; consistente con la postura general de mínimo privilegio y deny-by-default ya adoptada para onboarding (`REQ-AUTH-005`).
**Desventajas:** un usuario legítimo puede quedar temporalmente sin acceso ante una falla puntual y transitoria de infraestructura de directorio, hasta que el servicio se recupere.

## 6. Consecuencias

### Positivas
- Ninguna sesión se emite con autorización no verificada.
- Comportamiento consistente con la postura de seguridad ya adoptada en el resto del módulo.

### Negativas
- Menor disponibilidad ante fallos puntuales del subsistema de consulta de grupos de AD.

### Riesgos
- Si la infraestructura de consulta de grupos es inestable, este comportamiento puede generar fricción operativa perceptible. Mitigación: monitoreo del evento `AD_LOOKUP_FAILURE` como señal de salud de la integración AD, fuera del alcance de este ADR.

## 7. Áreas afectadas
Application (`AuthenticateADUser`, `SynchronizeADRoles`), API (`POST /auth/login`).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-002, REQ-AUTH-007
- Specifications: SPEC-AUTH-001 (RN-09, UC-AUTH-002, UC-AUTH-004)
- ADR relacionados: ADR-002, ADR-003
