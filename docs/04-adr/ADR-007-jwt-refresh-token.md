# ADR-007 — JWT (Access Token) + Refresh Token opaco para autenticación

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Se necesita un mecanismo de sesión que no requiera reenviar credenciales en cada request, con un único frontend propio (no múltiples clientes de terceros que requieran delegación de autorización).

## 4. Decisión
Se emite un Access Token JWT de corta duración (objetivo: 15 minutos) firmado criptográficamente, y un Refresh Token opaco de mayor duración (hipótesis inicial: 7 días) para renovar la sesión. No se implementa un Authorization Server OAuth2 completo.

## 5. Alternativas consideradas

### Alternativa A — OAuth2 Authorization Server completo
**Ventajas:** estándar de la industria para escenarios con múltiples clientes/terceros.
**Desventajas:** resuelve un problema (delegación a terceros) que no existe en este escenario; complejidad no justificada para V1.

### Alternativa B — JWT + Refresh Token opaco (elegida)
**Ventajas:** simple, suficiente para un frontend propio; separa claramente el formato del token (JWT) del problema que resuelve (sesión), sin adoptar todo el framework OAuth2.
**Desventajas:** requiere diseñar explícitamente rotación y revocación (no vienen "gratis" con un framework).

## 6. Consecuencias

### Positivas
- Complejidad proporcional a la necesidad real.

### Negativas
- Responsabilidad propia de diseñar rotación, revocación y transporte (ver ADR-008, ADR-010).

### Riesgos
- Si en el futuro se necesitan múltiples clientes de terceros con delegación, esta decisión deberá revisarse (candidato a ADR de reemplazo, no a extensión silenciosa).

## 7. Áreas afectadas
Domain (`RefreshToken`), Application (emisión/renovación), Infrastructure (`TokenPort` → adapter JWT).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-008
- Specifications: SPEC-AUTH-001, SPEC-AUTH-002
- ADR relacionados: ADR-006, ADR-008, ADR-009, ADR-010
