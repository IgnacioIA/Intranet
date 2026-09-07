# ADR-011 — Argon2id como algoritmo de hashing de contraseñas locales

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Las contraseñas de usuarios `LOCAL` deben almacenarse de forma que resistan un eventual acceso no autorizado a la base de datos, incluyendo ataques de fuerza bruta offline.

## 4. Decisión
Se utiliza Argon2id, con parámetros de memoria/iteraciones/paralelismo configurables, como único algoritmo de hashing de contraseñas `LOCAL`.

## 5. Alternativas consideradas

### Alternativa A — BCrypt
**Ventajas:** ampliamente soportado, probado en el tiempo.
**Desventajas:** más vulnerable que Argon2id frente a ataques acelerados por GPU a igual costo configurado.

### Alternativa B — Argon2id (elegida)
**Ventajas:** ganador de la Password Hashing Competition, diseñado específicamente para resistir ataques por hardware especializado.
**Desventajas:** requiere una librería adicional (no incluida por defecto en todos los stacks) y calibración cuidadosa de parámetros.

## 6. Consecuencias

### Positivas
- Mayor resistencia a ataques offline de fuerza bruta frente a la alternativa descartada.

### Negativas
- Necesidad de calibrar y documentar los parámetros elegidos según la capacidad del entorno de despliegue.

### Riesgos
- Ninguno relevante adicional.

## 7. Áreas afectadas
Infrastructure (`PasswordHasherPort` → adapter Argon2id).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-019
- Specifications: SPEC-AUTH-007, SPEC-AUTH-009
- Dominio: PasswordCredential
