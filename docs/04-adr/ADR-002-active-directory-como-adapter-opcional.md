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
Application (definición del Port), Infrastructure (adapter AD y adapter Fake de test).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-002, REQ-AUTH-021
- Specifications: SPEC-AUTH-001
- ADR relacionados: ADR-001
