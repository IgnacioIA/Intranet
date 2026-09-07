# ADR-014 — Integración con Spring Security confinada al borde de la aplicación

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Spring Security provee mecanismos estándar (`SecurityFilterChain`, `AuthenticationProvider`, `UserDetails`, `GrantedAuthority`) que resuelven buena parte de la integración HTTP de autenticación/autorización. Es necesario decidir cómo usarlos sin filtrar sus tipos hacia el dominio, y sin construir una capa artificialmente compleja para evitarlos por completo.

## 4. Decisión
Spring Security se utiliza como mecanismo de infraestructura en el borde de la aplicación: un `AuthenticationProvider` custom delega la verificación real en los Use Cases de Application (`AuthenticateLocalUser`/`AuthenticateADUser`) y traduce su resultado a un `Authentication` de Spring; `GrantedAuthority` se deriva de los `Permission` del dominio en el momento de construir el `SecurityContext`, nunca al revés. El dominio no importa ningún tipo de `org.springframework.security`.

## 5. Alternativas consideradas

### Alternativa A — Reimplementar autenticación/autorización HTTP sin Spring Security
**Ventajas:** cero dependencia del framework.
**Desventajas:** reconstruye mecanismos ya resueltos de forma robusta por el framework (manejo de filtros, contexto de seguridad, integración con anotaciones), sin beneficio real.

### Alternativa B — Usar Spring Security confinado al borde (elegida)
**Ventajas:** aprovecha mecanismos probados sin acoplar el dominio; evita reinventar infraestructura HTTP de seguridad.
**Desventajas:** exige disciplina para no dejar "escapar" tipos de Spring Security hacia application/domain.

## 6. Consecuencias

### Positivas
- Menor código propio de infraestructura HTTP de seguridad.
- Dominio verificable como independiente mediante ArchUnit.

### Negativas
- Ninguna significativa.

### Riesgos
- Riesgo de que, con el tiempo, algún desarrollador filtre un tipo de Spring Security hacia application/domain por conveniencia. Mitigación: regla de ArchUnit (ver `testing-strategy.md`).

## 7. Áreas afectadas
Infrastructure (REST layer, configuración de seguridad).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-021
- ADR relacionados: ADR-001, ADR-015
