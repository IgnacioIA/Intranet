# ADR-019 — Garantía de continuidad administrativa: bootstrap fail-fast y protección del último Master Admin

## 1. Estado
Aceptado

## 2. Fecha
2026-09-05

## 3. Contexto
El módulo depende de la existencia de al menos un administrador local (`SPEC-AUTH-009`) para poder operar la aplicación aun sin Active Directory disponible. Existen dos momentos en los que esa garantía puede romperse: (a) en el arranque, si nunca se configuró un mecanismo de bootstrap; (b) en operación normal, si un administrador deshabilita o elimina por error (o intencionalmente) la última cuenta administrativa activa. Es necesario decidir el comportamiento del sistema en ambos casos.

## 4. Decisión
1. **Arranque:** si al iniciar no existe ningún administrador local y tampoco hay un secreto externo de bootstrap válido configurado, la aplicación falla rápidamente (*fail-fast*) y no completa su arranque.
2. **Operación:** el sistema nunca permite, mediante un flujo normal de administración, que la última cuenta administrativa local activa quede deshabilitada, degradada o eliminada. Esta protección se implementa como una validación transaccional y bloqueante en el momento de la operación, no como una alerta posterior. Cuando sea necesario retirar una cuenta administrativa, se prefiere soft-deletion/deprovisioning (transición a `DEPROVISIONED`) frente a la eliminación física, para preservar el historial de auditoría.

**Addendum (2026-09-06, `SPEC-AUTH-010`):** el rol administrativo cuya continuidad protege esta decisión se formaliza como el rol de sistema `MASTER_ADMIN` (`isSystemRole = true`). La validación transaccional descrita en el punto 2 se expresa en el modelo de dominio como `INV-AUTH-013` y se extiende, por coherencia, a cubrir también el bloqueo (`LOCKED`) administrativo y la desactivación del propio Role `MASTER_ADMIN` — no solo deshabilitar/eliminar la cuenta — porque ambas acciones producen el mismo resultado operativo (ningún camino administrativo disponible). Esto no altera la decisión original: únicamente nombra el mecanismo y cierra un caso equivalente que no se había enumerado explícitamente.

## 5. Alternativas consideradas

### Alternativa A — Arrancar en un estado degradado sin administrador ni bootstrap configurado
**Ventajas:** no bloquea el despliegue inicial si la configuración de bootstrap se omitió por error.
**Desventajas:** un sistema en producción sin ningún camino administrativo disponible (y potencialmente sin AD accesible) es un riesgo operativo mayor que detener el arranque; el problema quedaría oculto hasta que alguien lo necesite y descubra que no existe.

### Alternativa B — Fail-fast en el arranque + protección transaccional del último administrador (elegida)
**Ventajas:** hace imposible, por diseño, llegar a un estado sin ningún camino administrativo; el fallo se detecta en el momento más barato de corregir (el arranque), no en medio de un incidente.
**Desventajas:** exige que el proceso de despliegue configure correctamente el secreto de bootstrap desde el primer arranque.

### Alternativa C — Alerta (no bloqueo) al intentar deshabilitar el último administrador
**Ventajas:** no restringe la operación del administrador.
**Desventajas:** una alerta puede ignorarse; no ofrece una garantía real, solo una advertencia.

## 6. Consecuencias

### Positivas
- Garantía estructural de que el sistema nunca queda sin ningún camino administrativo, ni en el arranque ni en operación.

### Negativas
- Un despliegue con configuración de bootstrap incompleta no arrancará, lo cual exige que el proceso de despliegue/infraestructura lo contemple explícitamente.

### Riesgos
- Ninguno relevante adicional, dado que la alternativa descartada (arrancar en estado degradado) es estrictamente más riesgosa.

## 7. Áreas afectadas
Infrastructure (proceso de arranque), Application (validación de operaciones administrativas sobre `User` con rol administrativo).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-016
- Specifications: SPEC-AUTH-009 (RN-07, RN-08, RN-09)
- ADR relacionados: ADR-011
