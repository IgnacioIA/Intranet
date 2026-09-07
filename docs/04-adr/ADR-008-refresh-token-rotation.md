# ADR-008 — Rotation de Refresh Tokens con detección de reuse

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
Un Refresh Token de larga vida robado permite mantener acceso indefinido hasta su expiración. Es necesario decidir si se adopta rotación (invalidar cada Refresh Token tras su uso, emitiendo uno nuevo) para poder detectar dicho robo.

## 4. Decisión
Se adopta Refresh Token Rotation: cada uso de un Refresh Token lo revoca y emite un reemplazo de la misma familia (`RefreshTokenFamily`). Si se presenta un token ya revocado, se interpreta como señal de robo: se revoca toda la familia y se exige nuevo login, registrando un evento de auditoría de severidad alta (`TOKEN_REUSE_DETECTED`).

La rotación es **estricta, sin grace period**: no existe una ventana de tolerancia que permita reutilizar el token inmediatamente anterior sin disparar la detección de reuse. Un segundo uso concurrente y legítimo del mismo token (ej. dos pestañas, un reintento de red) se trata igual que cualquier reutilización maliciosa: revoca toda la familia. La responsabilidad de evitar esta condición recae en el cliente/frontend, que debe serializar ("single-flight") sus llamadas de refresh.

## 5. Alternativas consideradas

### Alternativa A — Refresh Token estático, reutilizable hasta su expiración
**Ventajas:** más simple de implementar.
**Desventajas:** un token robado sirve durante toda su vida útil sin posibilidad de detección.

### Alternativa B — Rotation con detección de reuse (elegida)
**Ventajas:** detecta robo activo; limita la ventana de uso de un token robado a un único intercambio.
**Desventajas:** exige manejar concurrencia (dos requests simultáneos con el mismo token) mediante atomicidad transaccional (ver INV-AUTH-006, SPEC-AUTH-002).

### Alternativa C — Rotation con grace period (token anterior válido durante una ventana breve tras rotar)
**Ventajas:** tolera de forma transparente refresh concurrente legítimo sin penalizar al usuario con un logout forzado.
**Desventajas:** debilita la garantía de detección de robo (un atacante que roba el token dentro de esa ventana pasa desapercibido); añade complejidad de implementación (ventana temporal, doble validez). Descartada para V1: se prioriza la garantía de seguridad sobre la tolerancia a la concurrencia del cliente, trasladando esa responsabilidad al frontend (single-flight).

## 6. Consecuencias

### Positivas
- Detección activa de robo de sesión, no solo prevención pasiva.

### Negativas
- Complejidad adicional de diseño transaccional para evitar condiciones de carrera.

### Riesgos
- Una implementación incorrecta de la atomicidad podría permitir el uso doble de un mismo token o invalidar sesiones legítimas por una carrera mal resuelta. Mitigación: operación atómica única de "revocar + emitir hijo" (INV-AUTH-006), verificada con pruebas de concurrencia (ver `testing-strategy.md`).
- Sin grace period, un frontend que no serializa sus llamadas de refresh puede forzar logouts inesperados a sus propios usuarios. Mitigación: requisito explícito de single-flight en el cliente (documentado en `SPEC-AUTH-002`), no mitigado por el backend.

## 7. Áreas afectadas
Domain (`RefreshToken`, `RefreshTokenFamily`), Application (`RefreshAuthentication`), Persistence.

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-009
- Specifications: SPEC-AUTH-002
- Dominio: RefreshToken (INV-AUTH-006, INV-AUTH-007)
- ADR relacionados: ADR-007
