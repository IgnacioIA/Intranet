# Requisitos No Funcionales — Módulo de Seguridad (AUTH)

Contexto: `AUTH`.

---

# REQ-AUTH-019 — Hashing de contraseñas locales

## 1. Requisito
Toda contraseña de un usuario `LOCAL` debe almacenarse mediante Argon2id. No debe existir ningún mecanismo de almacenamiento reversible ni de hashing débil (MD5, SHA-1, SHA-256 sin salt/KDF).

## 2. Tipo
No funcional

## 3. Objetivo
Resistir ataques de fuerza bruta y de precómputo (rainbow tables) ante una eventual fuga de la base de datos.

## 4. Alcance
### Incluye
- Parámetros de Argon2id configurables (memoria, iteraciones, paralelismo).
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Ninguna contraseña es recuperable en texto plano a partir de lo almacenado.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-007`
- ADR relacionado: `ADR-011`

---

# REQ-AUTH-020 — Almacenamiento seguro de Refresh Tokens

## 1. Requisito
Los Refresh Tokens deben almacenarse hasheados. Nunca debe persistirse ni registrarse el valor en texto plano fuera del momento de su emisión al cliente.

## 2. Tipo
No funcional

## 3. Objetivo
Limitar el impacto de un acceso no autorizado a la base de datos.

## 4. Alcance
Incluye el hash del token y su metadata (family, expiración, estado). No incluye el valor original una vez emitido.

## 5. Criterios de aceptación
- Una consulta directa a la tabla de Refresh Tokens no permite reconstruir un token utilizable.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-009

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-002`

---

# REQ-AUTH-021 — Independencia del dominio respecto de infraestructura

## 1. Requisito
El dominio del módulo no debe depender de Spring Security, JPA/Hibernate, ninguna librería JWT concreta, ni de una implementación concreta de directorio (Active Directory/LDAP).

## 2. Tipo
No funcional

## 3. Objetivo
Permitir que un proyecto futuro reutilice únicamente la identidad `LOCAL` sin arrastrar dependencias de AD, y permitir sustituir infraestructura sin modificar el dominio.

## 4. Alcance
### Incluye
- Ports explícitos para directorio de identidad, persistencia, tokens, hashing de contraseña, auditoría y rate limiting.
### No incluye
- Empaquetado como Spring Boot Starter (postergado, ver ADR-016).

## 5. Criterios de aceptación
- Un análisis estático (ArchUnit) confirma que el paquete de dominio no importa clases de Spring, JPA, librerías JWT ni de directorio.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- ADR relacionado: `ADR-001`, `ADR-002`

---

# REQ-AUTH-022 — Operación en múltiples instancias

## 1. Requisito
El módulo debe poder operar correctamente cuando la aplicación se despliega en más de una instancia simultánea, sin depender de estado crítico exclusivo de la memoria de un proceso.

## 2. Tipo
No funcional

## 3. Objetivo
Evitar que el rate limiting o la revocación de sesiones dejen de funcionar correctamente al escalar horizontalmente.

## 4. Alcance
### Incluye
- Estado de autorización y de rate limiting en un almacenamiento compartido (base de datos en V1).
### No incluye
- Implementación de un almacenamiento distribuido dedicado (Redis) en V1 (ver ADR-013).

## 5. Criterios de aceptación
- El límite de intentos de login se respeta de forma agregada, independientemente de qué instancia atienda cada request.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-018

## 8. Trazabilidad
- ADR relacionado: `ADR-013`

---

# REQ-AUTH-023 — Prohibición de registro de secretos

## 1. Requisito
Ningún log técnico ni evento de auditoría debe contener contraseñas, Access Tokens, Refresh Tokens, secretos de firma o credenciales de Active Directory.

## 2. Tipo
No funcional

## 3. Objetivo
Evitar que la propia infraestructura de observabilidad se convierta en una fuente de fuga de credenciales.

## 4. Alcance
### Incluye
- Filtrado de campos sensibles a nivel de serialización de logs y de auditoría.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Una revisión de los serializers de logging/auditoría confirma la exclusión explícita de estos campos.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: todas las de este módulo.

---

# REQ-AUTH-024 — Trazabilidad de solicitudes (Correlation ID)

## 1. Requisito
Toda solicitud debe poder rastrearse mediante un identificador de correlación único, propagado a logs técnicos y eventos de auditoría relacionados.

## 2. Tipo
No funcional

## 3. Objetivo
Permitir reconstruir la secuencia completa de eventos de una solicitud durante un diagnóstico o una investigación de seguridad.

## 4. Alcance
Incluye generación o propagación de un `correlationId` por request. No incluye un sistema de tracing distribuido completo (APM) en V1.

## 5. Criterios de aceptación
- Un evento de auditoría y las líneas de log técnico de la misma solicitud comparten el mismo `correlationId`.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: todas las de este módulo.

---

# REQ-AUTH-025 — No enumeración de usuarios

## 1. Requisito
Ninguna respuesta de login, recuperación de contraseña o registro de error debe permitir inferir si una cuenta existe.

## 2. Tipo
No funcional

## 3. Objetivo
Evitar que un atacante pueda enumerar cuentas válidas mediante diferencias de respuesta, código HTTP o tiempo.

## 4. Alcance
Incluye mensajes, códigos HTTP y tiempos de respuesta equivalentes para "cuenta inexistente" y "credencial incorrecta". No incluye protección completa contra ataques de canal lateral de temporización a nivel de red.

## 5. Criterios de aceptación
- Una prueba automatizada confirma respuesta equivalente entre cuenta existente/inexistente en login y en recuperación de contraseña.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-001, REQ-AUTH-013

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`, `SPEC-AUTH-007`

---

# REQ-AUTH-026 — Migraciones de esquema versionadas

## 1. Requisito
La evolución del esquema de base de datos del módulo debe realizarse mediante migraciones versionadas (Flyway). No debe utilizarse generación automática de esquema (`ddl-auto=create`/`update`) como mecanismo de evolución.

## 2. Tipo
No funcional

## 3. Objetivo
Garantizar reproducibilidad, control de cambios y capacidad de auditar la evolución del esquema en un módulo con datos sensibles.

## 4. Alcance
Incluye migraciones versionadas desde el primer script. No incluye la definición del esquema final (pendiente de ADR/implementación).

## 5. Criterios de aceptación
- El esquema en cualquier entorno puede reconstruirse aplicando únicamente las migraciones versionadas.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- ADR relacionado: (a definir junto con el esquema concreto durante la implementación)
