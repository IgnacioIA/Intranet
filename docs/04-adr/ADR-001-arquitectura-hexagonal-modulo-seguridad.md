# ADR-001 — Arquitectura hexagonal (DDD + Ports & Adapters) para el módulo de seguridad

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
El módulo de seguridad debe integrar múltiples tecnologías de infraestructura (Spring Security, JPA/MySQL, una librería JWT, Active Directory) y se pretende, a futuro, reutilizable en proyectos que no necesiten todas ellas (por ejemplo, un proyecto que solo requiera identidad `LOCAL`). Es necesario decidir cómo aislar el comportamiento de negocio de estas tecnologías.

## 4. Decisión
El módulo se estructura en Domain, Application e Infrastructure, con Ports definidos desde Application/Domain e implementados por Adapters en Infrastructure. El dominio no depende de ninguna tecnología de infraestructura concreta.

## 5. Alternativas consideradas

### Alternativa A — Arquitectura en capas tradicional (Controller-Service-Repository) sin Ports explícitos
**Ventajas:** menor cantidad de interfaces, más rápida de escribir inicialmente.
**Desventajas:** el dominio termina dependiendo transitivamente de JPA/Spring Security; reutilizar sin AD o sin JPA exigiría refactorizar, no sustituir un adapter.

### Alternativa B — Hexagonal / Ports & Adapters (elegida)
**Ventajas:** el dominio es sustituible/reutilizable; ya existen dos implementaciones reales de `IdentityDirectoryPort` desde V1 (AD real, Fake para tests), lo que justifica la frontera sin especulación.
**Desventajas:** mayor cantidad de interfaces y mapeos en los bordes.

## 6. Consecuencias

### Positivas
- Un proyecto futuro puede usar solo la identidad `LOCAL` sin arrastrar AD.
- El dominio es testeable sin levantar Spring/JPA.

### Negativas
- Curva de entrada algo mayor para quien no conozca el patrón.

### Riesgos
- Sobre-abstracción si se crean Ports especulativos sin una segunda implementación real. Mitigación: un Port se crea únicamente cuando existe o se prevé con certeza una segunda implementación (ver §22 de `.claude/architecture/adr.md`).

## 7. Áreas afectadas
Todo el módulo de seguridad (domain, application, infrastructure).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-021
- Specifications: SPEC-AUTH-001 a SPEC-AUTH-009
- ADR relacionados: ADR-002, ADR-014, ADR-016
