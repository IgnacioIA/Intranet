# ADR-015 — Mecanismos para cross-cutting concerns: Spring Security Filters + Application Events (sin Decorator Pattern)

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Se había considerado inicialmente el Decorator Pattern como mecanismo único para separar autenticación, autorización, logging técnico y auditoría. Es necesario evaluar si ese patrón aporta valor real frente a mecanismos ya idiomáticos del ecosistema Spring, o si constituye una elección "porque suena apropiada" sin justificación concreta.

## 4. Decisión
No se adopta un mecanismo único para todos los cross-cutting concerns. Se asigna el mecanismo más simple y directo a cada uno: Spring Security Filters + `AuthenticationProvider` para autenticación; un servicio de aplicación explícito para autorización por permiso; un `Filter` servlet dedicado para Correlation ID; Application/Domain Events con un listener para disparar auditoría. No se utiliza Decorator Pattern ni AOP genérico como mecanismo transversal.

## 5. Alternativas consideradas

### Alternativa A — Decorator Pattern envolviendo servicios de aplicación
**Ventajas:** explícito en el código de composición.
**Desventajas:** para varios de estos concerns (autenticación HTTP, correlation ID por request) ya existen mecanismos más idiomáticos y simples (filtros); usar Decorator igualmente sería una capa adicional sin beneficio de separación real.

### Alternativa B — AOP genérico para todos los concerns
**Ventajas:** centraliza el cross-cutting en un solo paradigma.
**Desventajas:** varios de estos concerns son por-request (HTTP), no por-invocación-de-método; un `Filter` es más simple y directo que interceptar métodos vía AOP para ese caso.

### Alternativa C — Mecanismo específico por concern (elegida)
**Ventajas:** cada concern usa la herramienta más simple que lo resuelve correctamente; máxima claridad y testabilidad.
**Desventajas:** menor "uniformidad" estilística (varios mecanismos en juego), aceptada como consecuencia de evitar una abstracción única forzada.

## 6. Consecuencias

### Positivas
- Cada concern es fácil de entender y probar de forma aislada.
- Se evita una capa de indirección (Decorator) sin beneficio real de separación.

### Negativas
- Un desarrollador nuevo debe conocer varios mecanismos en lugar de uno solo.

### Riesgos
- Ninguno relevante adicional.

## 7. Áreas afectadas
Infrastructure (filtros, configuración de Spring Security), Application (servicio de autorización, listeners de eventos).

## 8. Documentación relacionada
- Specifications: SPEC-AUTH-006
- ADR relacionados: ADR-012, ADR-014
