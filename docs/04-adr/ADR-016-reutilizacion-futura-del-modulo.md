# ADR-016 — Estrategia de reutilización futura del módulo (JAR interno antes que Spring Boot Starter)

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
El módulo se diseña con arquitectura hexagonal para permitir reutilización futura en otros proyectos Java/Spring Boot. Es necesario decidir el orden de evolución hacia esa reutilización sin sobreingeniería prematura.

## 4. Decisión
El módulo permanece, en V1, como parte del proyecto actual (un paquete propio, sin extracción). La extracción a un artefacto Maven (JAR) independiente, y eventualmente a un Spring Boot Starter, se evalúa únicamente cuando exista un segundo consumidor real del módulo.

## 5. Alternativas consideradas

### Alternativa A — Extraer como Spring Boot Starter desde V1
**Ventajas:** reutilización inmediata "lista para usar" en cualquier proyecto futuro.
**Desventajas:** sobreingeniería sin un segundo consumidor real que valide los puntos de extensión necesarios; riesgo de diseñar configurabilidad especulativa incorrecta.

### Alternativa B — Permanecer dentro del proyecto actual hasta que exista un segundo consumidor (elegida)
**Ventajas:** evita diseñar puntos de extensión sin evidencia real de necesidad; la arquitectura hexagonal ya deja el camino preparado para la extracción cuando corresponda.
**Desventajas:** la extracción futura requerirá un esfuerzo dedicado cuando llegue el momento.

## 6. Consecuencias

### Positivas
- Se evita configurabilidad prematura y mal calibrada.

### Negativas
- Postergar la extracción implica un esfuerzo futuro no trivial (aunque reducido por la arquitectura hexagonal ya adoptada).

### Riesgos
- Ninguno relevante si se respeta la disciplina de Ports & Adapters ya establecida (ADR-001).

## 7. Áreas afectadas
Empaquetado del proyecto (fuera del alcance de V1).

## 8. Documentación relacionada
- ADR relacionados: ADR-001
