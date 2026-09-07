# ADR-009 — Firma asimétrica del JWT con soporte de `kid` para rotación futura de claves

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
El Access Token debe firmarse de forma verificable. Es necesario decidir el esquema de firma pensando en que, eventualmente, será necesario rotar la clave sin invalidar de golpe todos los tokens en circulación ni forzar un rediseño posterior.

## 4. Decisión
El Access Token se firma con un algoritmo asimétrico (ej. RS256/ES256), incluyendo el header `kid` desde V1, aunque exista una única clave activa. Esto permite, en el futuro, publicar múltiples claves (JWKS) y rotar sin interrumpir tokens emitidos con la clave anterior hasta su expiración natural.

## 5. Alternativas consideradas

### Alternativa A — Firma simétrica (HMAC/HS256) con clave única
**Ventajas:** más simple de configurar inicialmente.
**Desventajas:** la clave de verificación es la misma que la de firma; cualquier componente que deba verificar el token debe poseer el secreto, ampliando la superficie de exposición; sin `kid`, rotar exige un rediseño posterior.

### Alternativa B — Firma asimétrica + `kid` desde V1 (elegida)
**Ventajas:** los verificadores solo necesitan la clave pública; rotación de clave sin rediseño futuro.
**Desventajas:** gestión de par de claves algo más compleja que un secreto único.

## 6. Consecuencias

### Positivas
- Rotación de claves posible sin invalidar tokens vigentes ni cambiar el formato del token.

### Negativas
- Gestión de claves (almacenamiento seguro de la clave privada) desde V1.

### Riesgos
- Ninguno relevante adicional frente a la alternativa descartada.

## 7. Áreas afectadas
Infrastructure (`TokenPort` → adapter JWT, gestión de claves).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-008
- Specifications: SPEC-AUTH-001
- ADR relacionados: ADR-007
