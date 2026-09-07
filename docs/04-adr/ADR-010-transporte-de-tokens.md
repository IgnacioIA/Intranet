# ADR-010 — Transporte de tokens: Refresh Token en cookie httpOnly, Access Token en body

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Frontend y backend se despliegan bajo el mismo sitio. Es necesario decidir dónde vive cada token en el cliente, dado que esta decisión determina la exposición ante XSS y CSRF.

## 4. Decisión
El Refresh Token se transporta en una cookie `HttpOnly`, `Secure`, `SameSite` apropiado para mismo sitio, con `Path=/auth`. El Access Token se devuelve en el cuerpo de la respuesta y el frontend lo mantiene en memoria (nunca `localStorage`).

**Decision Ledger 2026-09-06 (Fase 22, Security Review):** el valor de `Path` originalmente aprobado aquí era `/auth/refresh`. Se corrige a `/auth` porque, por RFC 6265 §5.1.4, un cookie con `Path=/auth/refresh` nunca se envía en requests a `/auth/logout` ni `/auth/logout/all` (no comparten ese prefijo) — el navegador jamás habría adjuntado el Refresh Token a esos dos endpoints, impidiendo su revocación real en el servidor pese a que SPEC-AUTH-003 exige exactamente eso. `Path=/auth` cubre los cuatro endpoints que necesitan leer o fijar la cookie (`login`, `refresh`, `logout`, `logout/all`) sin exponerla fuera de este controlador. El riesgo de CSRF señalado en la sección 5 (mitigado por `SameSite` + `Path`) sigue vigente con el nuevo valor: `/auth` sigue siendo un alcance acotado, no `/`.

## 5. Alternativas consideradas

### Alternativa A — Ambos tokens en el cuerpo de la respuesta, gestionados por el frontend (ej. `localStorage`)
**Ventajas:** más simple de implementar en el frontend.
**Desventajas:** cualquier XSS exitoso puede exfiltrar ambos tokens directamente.

### Alternativa B — Refresh Token en cookie httpOnly, Access Token en memoria (elegida)
**Ventajas:** el Refresh Token, el de mayor duración y por tanto el de mayor impacto si se roba, no es accesible desde JavaScript.
**Desventajas:** requiere considerar CSRF sobre el endpoint de refresh (mitigado por `SameSite` al ser mismo sitio, y por el alcance restringido de `Path`).

## 6. Consecuencias

### Positivas
- Reduce significativamente la superficie de robo de Refresh Token vía XSS.

### Negativas
- El Access Token en memoria se pierde al recargar la página, exigiendo un flujo de "silent refresh" al iniciar la aplicación en el frontend.

### Riesgos
- Si en el futuro frontend y backend dejan de ser mismo sitio (cross-site), esta decisión debe revisarse explícitamente (impacta `SameSite` y CORS). Ver Preguntas Abiertas del resumen ejecutivo.

## 7. Áreas afectadas
API Layer (headers/cookies de respuesta), frontend (fuera del alcance de este módulo, pero condiciona su contrato).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-008, REQ-AUTH-009
- Specifications: SPEC-AUTH-001, SPEC-AUTH-002
- ADR relacionados: ADR-007
