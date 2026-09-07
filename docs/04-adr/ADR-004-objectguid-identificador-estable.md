# ADR-004 — `objectGUID` como identificador externo estable de usuarios AD

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Es necesario correlacionar de forma estable un usuario de Active Directory con su Shadow Identity local. El `username`/`sAMAccountName`/UPN puede cambiar (rename) o, en teoría, reasignarse a otra persona tras eliminarse la cuenta original, lo que generaría pérdida de historial o, peor, herencia indebida de identidad/roles.

## 4. Decisión
Se utiliza `objectGUID` (identificador binario inmutable asignado por AD al crear el objeto, nunca reutilizado) como `external_id` de la Shadow Identity. El `username` se almacena como atributo mutable de conveniencia (login/visualización), nunca como clave.

## 5. Alternativas consideradas

### Alternativa A — Usar `sAMAccountName`/UPN como clave
**Ventajas:** más simple de leer/depurar.
**Desventajas:** mutable; un rename crea una identidad "nueva" o, si se reutiliza un username liberado, puede fusionar identidades de personas distintas.

### Alternativa B — `objectGUID` (elegida)
**Ventajas:** inmutable, nunca reutilizado, es la clave recomendada para este propósito en el ecosistema AD.
**Desventajas:** menos legible para depuración manual (requiere resolución adicional para mostrarlo como texto).

## 6. Consecuencias

### Positivas
- Un rename en AD no rompe ni duplica la identidad local.
- Elimina el riesgo de herencia de identidad por reutilización de username.

### Negativas
- Ninguna significativa.

### Riesgos
- Si el usuario es eliminado y recreado en AD, obtiene un nuevo `objectGUID`: la aplicación lo tratará correctamente como una identidad nueva (comportamiento intencional, no un defecto).

## 7. Áreas afectadas
Domain (`User.externalId`), Infrastructure (`ActiveDirectoryAdapter`).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-003
- Specifications: SPEC-AUTH-001
- Dominio: User (INV-AUTH-002)
- ADR relacionados: ADR-003
