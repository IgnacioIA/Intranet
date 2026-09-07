# ADR-013 — RateLimitPort preparado para adapter distribuido (Redis) sin implementarlo en V1

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
El sistema debe limitar intentos de autenticación (IP + identidad) y debe poder operar en múltiples instancias, por lo que un contador en memoria de un único proceso es inválido. Introducir Redis en V1 agregaría una dependencia de infraestructura no justificada todavía por la escala esperada.

## 4. Decisión
Se define un `RateLimiterPort` en la capa de aplicación, con una implementación V1 respaldada por base de datos (conteo agregado con ventana de tiempo), suficiente para múltiples instancias sin nueva infraestructura. El puerto queda preparado para una futura implementación con Redis si el volumen lo justifica, sin cambios en el dominio ni en la aplicación.

## 5. Alternativas consideradas

### Alternativa A — Contador en memoria del proceso
**Ventajas:** trivial de implementar.
**Desventajas:** inválido en despliegue multi-instancia (REQ-AUTH-022); cada instancia contaría por separado.

### Alternativa B — Redis desde V1
**Ventajas:** rendimiento óptimo para conteo distribuido.
**Desventajas:** introduce una dependencia de infraestructura nueva sin evidencia de que la escala actual lo requiera; sobreingeniería para V1.

### Alternativa C — Base de datos con Port preparado para Redis futuro (elegida)
**Ventajas:** correcto para multi-instancia sin nueva infraestructura; migración a Redis después es un cambio de adapter, no de arquitectura.
**Desventajas:** menor rendimiento que Redis bajo carga alta (aceptable a la escala prevista de V1).

## 6. Consecuencias

### Positivas
- Correcto en multi-instancia desde V1 sin nueva infraestructura.

### Negativas
- Rendimiento inferior a una solución dedicada bajo carga alta.

### Riesgos
- Si el volumen de intentos de login crece significativamente, la tabla de conteo podría requerir optimización (índices, particionado) antes de justificar Redis.

## 7. Áreas afectadas
Application (`RateLimiterPort`), Infrastructure (adapter DB), Persistence.

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-018, REQ-AUTH-022
- Specifications: SPEC-AUTH-001
