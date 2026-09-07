# ADR-018 — Provider explícito en el request de login (sin autodetección por username)

## 1. Estado
Aceptado

## 2. Fecha
2026-09-05

## 3. Contexto
El endpoint de login debe determinar si una credencial corresponde a un usuario `LOCAL` o `ACTIVE_DIRECTORY`. Existen dos identidades potencialmente distintas bajo el mismo `username` (INV-AUTH-001: la unicidad es por proveedor, no global). Es necesario decidir si esa resolución la hace el sistema de forma automática o si la declara explícitamente el cliente.

## 4. Decisión
El campo `provider` es obligatorio en el request de `POST /auth/login`. El sistema no intenta inferir el proveedor a partir del `username` (ni probando un proveedor y luego otro, ni mediante convenciones de prefijo/dominio en el nombre de usuario).

## 5. Alternativas consideradas

### Alternativa A — Autodetección probando LOCAL y luego ACTIVE_DIRECTORY
**Ventajas:** el cliente no necesita conocer ni enviar el proveedor.
**Desventajas:** introduce ambigüedad de comportamiento cuando existen namespaces separados; el orden de verificación (¿LOCAL primero o AD primero?) es una decisión arbitraria adicional; puede introducir diferencias de tiempo de respuesta entre proveedores explotables como canal lateral; complica el manejo de errores (¿qué mensaje corresponde si ambos proveedores rechazan la credencial por razones distintas?).

### Alternativa B — Provider explícito en el request (elegida)
**Ventajas:** elimina la ambigüedad y el canal lateral de temporización entre proveedores; simplifica la lógica de login a un único camino determinístico por solicitud; hace explícito en el contrato de API qué proveedor se está utilizando, mejorando la trazabilidad de los eventos de auditoría desde el primer instante.
**Desventajas:** el frontend debe conocer y ofrecer la selección de proveedor (o inferirla de otra señal de UX, como un selector o un dominio de correo), trasladando esa responsabilidad fuera del backend.

## 6. Consecuencias

### Positivas
- Comportamiento de login determinístico y sin ambigüedad entre namespaces.
- Elimina un posible canal lateral de temporización entre proveedores.

### Negativas
- El frontend debe resolver la UX de selección de proveedor (fuera del alcance de este módulo).

### Riesgos
- Ninguno relevante adicional.

## 7. Áreas afectadas
API (`POST /auth/login`), Application (`AuthenticateLocalUser`, `AuthenticateADUser`).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-001, REQ-AUTH-002
- Specifications: SPEC-AUTH-001 (RN-08, API Contract)
- Dominio: User (INV-AUTH-001)
