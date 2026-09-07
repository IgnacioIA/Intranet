# Requisitos Funcionales — Módulo de Seguridad (AUTH)

Contexto: `AUTH`. Ver [`GLOSSARY.md`](../GLOSSARY.md) para vocabulario y `.claude/documentation/conventions.md` para el formato de identificadores.

---

# REQ-AUTH-001 — Autenticación de usuarios locales

## 1. Requisito
El sistema debe permitir autenticar usuarios cuyas credenciales son gestionadas por la propia aplicación (provider `LOCAL`).

## 2. Tipo
Funcional

## 3. Objetivo
Permitir el acceso a usuarios que no dependen de Active Directory (por ejemplo, administradores locales de break-glass).

## 4. Alcance
### Incluye
- Verificación de usuario y contraseña contra credenciales almacenadas por la aplicación.
### No incluye
- Gestión de contraseñas de Active Directory.

## 5. Criterios de aceptación
- Un usuario `LOCAL` con credenciales correctas obtiene una sesión válida.
- Un usuario `LOCAL` con credenciales incorrectas es rechazado sin distinguir la causa exacta del rechazo.

## 6. Restricciones
- Ninguna contraseña `LOCAL` puede almacenarse de forma reversible.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-002 — Autenticación de usuarios de Active Directory

## 1. Requisito
El sistema debe permitir autenticar usuarios cuyas credenciales son gestionadas por Active Directory (provider `ACTIVE_DIRECTORY`), delegando en AD la verificación de la contraseña.

## 2. Tipo
Funcional

## 3. Objetivo
Reutilizar la autoridad de identidad ya existente en la organización sin duplicar la gestión de credenciales.

## 4. Alcance
### Incluye
- Verificación de credenciales contra AD; obtención de `objectGUID` y grupos del usuario.
### No incluye
- Gestión, cambio o recuperación de contraseñas de AD desde la aplicación (ver REQ-AUTH-012).

## 5. Criterios de aceptación
- Un usuario AD con credenciales correctas obtiene una sesión válida.
- Si AD no está disponible, el sistema informa una condición distinguible de "credenciales incorrectas".

## 6. Restricciones
- El dominio no debe depender de una implementación concreta de AD/LDAP (ver ADR-002).

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-003 — Identidad estable para usuarios de Active Directory

## 1. Requisito
El sistema debe correlacionar la identidad de un usuario AD mediante un identificador externo estable e inmutable, no mediante su nombre de usuario.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar pérdida de historial, de roles o colisión de identidad ante un cambio de nombre de usuario en AD.

## 4. Alcance
### Incluye
- Uso de `objectGUID` como `external_id`.
### No incluye
- Sincronización de atributos de AD no necesarios para la aplicación.

## 5. Criterios de aceptación
- Un cambio de nombre de usuario en AD no genera una nueva identidad dentro de la aplicación.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-002

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-004 — Frontera entre grupos de Active Directory y autorización de la aplicación

## 1. Requisito
La pertenencia a un grupo de Active Directory no debe traducirse automáticamente en permisos de la aplicación. Debe existir un mapping explícito, administrable, entre grupo AD y rol de la aplicación.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar que cambios en la estructura de grupos de AD, fuera del control de esta aplicación, modifiquen silenciosamente privilegios dentro de ella.

## 4. Alcance
### Incluye
- Mapping `AD Group → Application Role` almacenado en base de datos.
- Administración del mapping protegida por permisos administrativos.
- Auditoría de cambios sobre el mapping.
### No incluye
- Interpretación automática de la semántica del nombre de un grupo AD.

## 5. Criterios de aceptación
- Un grupo AD sin mapping conocido no otorga ningún rol.
- Todo cambio al mapping queda auditado con su autor.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-002

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`, `SPEC-AUTH-008`

---

# REQ-AUTH-005 — Onboarding de usuarios de Active Directory nuevos

## 1. Requisito
Un usuario AD que se autentica correctamente por primera vez, y que no posee ningún grupo mapeado a un rol válido, debe recibir únicamente acceso a una capacidad mínima de onboarding — nunca acceso general por defecto.

## 2. Tipo
Funcional

## 3. Objetivo
Aplicar mínimo privilegio y deny-by-default ante identidades nuevas.

## 4. Alcance
### Incluye
- Aprovisionamiento automático de la Shadow Identity.
- Asignación del rol `ONBOARDING_USER` cuando no exista ningún mapping válido.
### No incluye
- Aprobación manual de cada usuario nuevo (la aprobación ya existe implícita en el mapping del grupo, cuando lo hay).

## 5. Criterios de aceptación
- Un usuario AD nuevo sin grupos mapeados solo puede acceder a la información de onboarding.
- Un usuario AD nuevo con al menos un grupo mapeado queda `ACTIVE` automáticamente con el/los roles correspondientes.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-002, REQ-AUTH-004

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-006 — Autorización basada en roles y permisos (RBAC)

## 1. Requisito
El sistema debe permitir asignar múltiples roles a un usuario y múltiples permisos a un rol. La autorización se concede únicamente cuando el usuario posee, mediante alguno de sus roles, el permiso requerido por la acción.

## 2. Tipo
Funcional

## 3. Objetivo
Proveer un modelo de autorización simple, auditable y suficiente para las necesidades actuales, sin introducir un motor de políticas complejo.

## 4. Alcance
### Incluye
- Roles, permisos, y su asociación.
- Semántica aditiva (ausencia de permiso = denegado; no existen reglas de denegación explícita).
### No incluye
- Autorización por atributo, por recurso u ownership (ver exclusiones de V1).

## 5. Criterios de aceptación
- Un usuario sin el permiso requerido recibe autorización denegada.
- Un usuario con el permiso requerido, mediante cualquiera de sus roles, recibe autorización concedida.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-006`

---

# REQ-AUTH-007 — Doble procedencia de asignación de roles

## 1. Requisito
El sistema debe distinguir los roles derivados automáticamente del mapping de Active Directory de los roles asignados explícitamente por un administrador de la aplicación. Un nuevo inicio de sesión AD no debe eliminar roles asignados explícitamente.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar autorización obsoleta (roles AD no revisados) sin perder el control administrativo directo de la aplicación.

## 4. Alcance
### Incluye
- Recalcular en cada login AD exitoso los roles de procedencia `DERIVED_FROM_AD`.
- Persistir sin alteración los roles de procedencia `GRANTED_EXPLICITLY`.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Remover a un usuario de un grupo AD elimina, en su próximo login, el rol derivado correspondiente.
- Un rol asignado explícitamente permanece después de cualquier sincronización AD.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-002, REQ-AUTH-004, REQ-AUTH-006

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`, `SPEC-AUTH-006`

---

# REQ-AUTH-008 — Emisión de sesión mediante tokens

## 1. Requisito
El sistema debe emitir, tras una autenticación exitosa, un token de acceso de corta duración y un token de refresco de mayor duración, para evitar el reenvío repetido de credenciales.

## 2. Tipo
Funcional

## 3. Objetivo
Sostener la sesión del usuario de forma segura y eficiente.

## 4. Alcance
### Incluye
- Emisión de Access Token (JWT) y Refresh Token (opaco) al autenticar.
### No incluye
- Un Authorization Server OAuth2 completo.

## 5. Criterios de aceptación
- Toda autenticación exitosa produce ambos tokens.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-001 o REQ-AUTH-002

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-009 — Renovación de sesión sin repetir credenciales

## 1. Requisito
El sistema debe permitir obtener un nuevo Access Token mediante un Refresh Token válido, sin requerir nuevamente las credenciales originales, y debe rotar el Refresh Token en cada uso.

## 2. Tipo
Funcional

## 3. Objetivo
Mantener la sesión activa minimizando el riesgo de robo prolongado de un Refresh Token.

## 4. Alcance
### Incluye
- Rotación con family/lineage y detección de reutilización.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Un Refresh Token válido y no utilizado produce un nuevo par de tokens y revoca el anterior.
- La reutilización de un Refresh Token ya revocado revoca toda su familia y exige nuevo login.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-008

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-002`

---

# REQ-AUTH-010 — Cierre de sesión

## 1. Requisito
El sistema debe permitir cerrar la sesión activa (revocando su Refresh Token) y, opcionalmente, cerrar todas las sesiones del usuario en todos los dispositivos.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir al usuario finalizar su sesión y responder a la sospecha de que otra sesión no es de su control.

## 4. Alcance
### Incluye
- Logout individual y logout de todas las sesiones.
### No incluye
- Invalidación inmediata del Access Token ya emitido (expira naturalmente).

## 5. Criterios de aceptación
- Tras logout individual, el Refresh Token correspondiente queda revocado.
- Tras logout-all, todas las familias de Refresh Token del usuario quedan revocadas.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-008

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-003`

---

# REQ-AUTH-011 — Revocación administrativa de sesiones

## 1. Requisito
Un administrador con el permiso correspondiente debe poder revocar todas las sesiones activas de un usuario determinado.

## 2. Tipo
Funcional

## 3. Objetivo
Responder a incidentes de seguridad (cuenta comprometida, egreso de personal) sin depender de intervención directa en base de datos.

## 4. Alcance
### Incluye
- Revocación de todas las familias de Refresh Token del usuario objetivo.
### No incluye
- Invalidación inmediata de Access Tokens ya emitidos.

## 5. Criterios de aceptación
- La operación queda registrada en Security Audit con el administrador que la ejecutó y el usuario afectado.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-006, REQ-AUTH-008

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-004`

---

# REQ-AUTH-012 — Exclusión de gestión de contraseñas de Active Directory

## 1. Requisito
El sistema no debe implementar cambio ni recuperación de contraseña para usuarios `ACTIVE_DIRECTORY`.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar duplicar o suplantar una responsabilidad que pertenece exclusivamente a AD.

## 4. Alcance
### Incluye
- Rechazo explícito de estas operaciones para usuarios AD, con mensaje que oriente al canal correspondiente de la organización.
### No incluye
Ninguna implementación de dichas operaciones.

## 5. Criterios de aceptación
- Un usuario AD que solicita cambio/recuperación de contraseña recibe una respuesta que indica que debe gestionarlo por el canal de AD/IT, sin revelar detalles internos.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-002

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-007`

---

# REQ-AUTH-013 — Gestión de contraseña para usuarios locales

## 1. Requisito
El sistema debe permitir a un usuario `LOCAL` cambiar su contraseña estando autenticado, y recuperarla mediante un proceso de dos pasos cuando la haya olvidado.

## 2. Tipo
Funcional

## 3. Objetivo
Dar autonomía a los usuarios locales sin intervención administrativa para el caso común de contraseña olvidada.

## 4. Alcance
### Incluye
- Cambio de contraseña autenticado.
- Solicitud y confirmación de recuperación mediante token de un solo uso.
### No incluye
- Recuperación para usuarios AD (ver REQ-AUTH-012).

## 5. Criterios de aceptación
- La solicitud de recuperación responde de forma equivalente exista o no la cuenta asociada al correo indicado.
- El token de recuperación es de un solo uso y expira.

## 6. Restricciones
- Contraseñas almacenadas mediante Argon2id (ver REQ-AUTH-017).

## 7. Dependencias
- REQ-AUTH-001

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-007`

---

# REQ-AUTH-014 — Consulta de identidad y autorización actual

## 1. Requisito
El sistema debe exponer un mecanismo para que el frontend obtenga la identidad, estado y permisos del usuario autenticado, sin necesidad de interpretar directamente el contenido del token.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar que el frontend dependa del formato interno del Access Token para decidir qué mostrar u ocultar.

## 4. Alcance
### Incluye
- Identidad, estado de cuenta, roles y permisos vigentes.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- La respuesta refleja el estado y permisos actuales en base de datos, no un valor congelado del token.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-006

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-005`

---

# REQ-AUTH-015 — Autorización viva contra base de datos

## 1. Requisito
Toda solicitud protegida debe evaluarse contra el estado, roles y permisos actuales del usuario en base de datos. Un cambio de permisos, de roles o de estado de cuenta debe tener efecto desde la siguiente solicitud, sin depender de que expire un token.

## 2. Tipo
Funcional

## 3. Objetivo
Evitar autorización obsoleta congelada dentro de un JWT de larga vida efectiva.

## 4. Alcance
### Incluye
- Consulta de estado/roles/permisos en cada request protegido.
### No incluye
- Invalidación retroactiva de un Access Token ya emitido dentro de su ventana de vigencia (ver REQ-AUTH-010 y ADR-006).

## 5. Criterios de aceptación
- Deshabilitar una cuenta impide el acceso desde el siguiente request, aunque su Access Token siga siendo válido criptográficamente.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-006

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-006`

---

# REQ-AUTH-016 — Administrador local independiente de Active Directory

## 1. Requisito
Debe existir al menos una cuenta administrativa `LOCAL` capaz de operar la aplicación aunque Active Directory esté completamente indisponible, con un mecanismo de arranque (bootstrap) que no dependa de una contraseña embebida en el código.

## 2. Tipo
Funcional

## 3. Objetivo
Garantizar administración de la aplicación sin depender de un sistema externo, sin introducir una vulnerabilidad de contraseña fija.

## 4. Alcance
### Incluye
- Bootstrap mediante secreto externo (hash inyectado en el arranque), cambio de contraseña obligatorio en el primer uso.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- El sistema nunca contiene una contraseña administrativa en texto plano dentro del código fuente.
- El sistema no puede quedar, mediante un flujo normal, sin ningún administrador local activo.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-009`

---

# REQ-AUTH-017 — Auditoría de seguridad

## 1. Requisito
El sistema debe registrar eventos semánticos de seguridad (autenticación, autorización, gestión de tokens, gestión de contraseña, administración) de forma distinguible de los logs técnicos.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir reconstruir qué ocurrió, cuándo y por quién, con fines de seguridad y cumplimiento.

## 4. Alcance
### Incluye
- Catálogo de eventos definido en `docs/03-architecture/security.md`.
### No incluye
- Envío a un SIEM externo (postergado).

## 5. Criterios de aceptación
- Todo evento del catálogo queda registrado con marca de tiempo UTC y `correlationId`.

## 6. Restricciones
- Nunca se registran contraseñas, tokens ni secretos (ver REQ-AUTH-021).

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: todas las de este módulo.

---

# REQ-AUTH-018 — Limitación de intentos de autenticación

## 1. Requisito
El sistema debe limitar la tasa de intentos de autenticación por IP y por identidad, para mitigar ataques de fuerza bruta y credential stuffing.

## 2. Tipo
Funcional

## 3. Objetivo
Reducir la efectividad de ataques automatizados contra el login.

## 4. Alcance
### Incluye
- Límite combinado IP + identidad.
### No incluye
- Un sistema de reputación de IP externo.

## 5. Criterios de aceptación
- Superado el umbral, el sistema responde con una condición de límite excedido, auditada.

## 6. Restricciones
- El estado de conteo no puede vivir únicamente en memoria de una única instancia (ver REQ-AUTH-020).

## 7. Dependencias
- REQ-AUTH-001, REQ-AUTH-002

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-001`

---

# REQ-AUTH-027 — Administración de identidades locales

## 1. Requisito
Un administrador con el permiso correspondiente debe poder consultar (con filtros, búsqueda y paginación), crear y actualizar identidades `LOCAL`.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir operar el modelo de usuarios sin intervención directa en base de datos.

## 4. Alcance
### Incluye
- Consulta de usuarios (LOCAL y AD) con filtros por estado, proveedor y texto.
- Alta y actualización de identidad (email, nombre visible) de usuarios `LOCAL`.
### No incluye
- Alta manual de usuarios `ACTIVE_DIRECTORY` (su Shadow Identity se crea únicamente vía login, `SPEC-AUTH-001` UC-AUTH-003).
- Actualización de atributos de identidad de usuarios AD (se sincronizan desde AD, no se editan administrativamente).

## 5. Criterios de aceptación
- Un administrador sin el permiso requerido no puede listar, crear ni actualizar usuarios.
- Un intento de creación manual de un usuario `ACTIVE_DIRECTORY` es rechazado.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-028 — Cambio de estado administrativo de un usuario

## 1. Requisito
Un administrador con el permiso correspondiente debe poder habilitar, deshabilitar, bloquear, desbloquear o deprovisionar un usuario, con efecto inmediato sobre su capacidad de autenticarse y operar.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir operar el ciclo de vida de una cuenta (incidentes, egresos, reactivaciones) sin intervención directa en base de datos.

## 4. Alcance
### Incluye
- Transiciones administrativas entre los estados ya aprobados (`PENDING_ONBOARDING`, `ACTIVE`, `LOCKED`, `DISABLED`, `DEPROVISIONED`), respetando las transiciones válidas ya definidas en `02-domain/transitions.md`.
### No incluye
- Nuevos estados de usuario.
- Reversión de `DEPROVISIONED` (estado terminal, ya definido como tal).

## 5. Criterios de aceptación
- Deshabilitar/bloquear/deprovisionar un usuario revoca sus sesiones activas y le impide operar desde la siguiente solicitud.
- Reactivar un usuario reevalúa sus roles vigentes en lugar de asumir ciegamente `ACTIVE`.
- Ninguna de estas operaciones puede dejar al sistema sin ningún `User` `ACTIVE` con el rol `MASTER_ADMIN` (ver REQ-AUTH-033).

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-015, REQ-AUTH-033

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-029 — Administración de Roles

## 1. Requisito
Un administrador con el permiso correspondiente debe poder crear Roles, modificar su descripción y su conjunto de Permissions, y activarlos/desactivarlos. Un Role nunca se elimina físicamente.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir evolucionar el modelo de autorización sin perder integridad histórica del Security Audit ni de las asignaciones existentes.

## 4. Alcance
### Incluye
- Alta, modificación y activación/desactivación de Roles.
### No incluye
- Eliminación física de un Role.
- Desactivación de Roles de sistema (`isSystemRole = true`).

## 5. Criterios de aceptación
- Un Role desactivado deja de otorgar permisos de forma inmediata, sin eliminar las asignaciones existentes.
- Un intento de desactivar un Role de sistema, o el Role `MASTER_ADMIN` cuando sea el único que sostiene la continuidad administrativa, es rechazado.

## 6. Restricciones
- Ver `ADR-020` (soft deactivation).

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-030 — Administración de Permissions

## 1. Requisito
Un administrador con el permiso correspondiente debe poder crear Permissions, modificar su descripción y activarlas/desactivarlas. El nombre de una Permission es inmutable y nunca se elimina físicamente.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir evolucionar el catálogo de permisos sin perder integridad histórica.

## 4. Alcance
### Incluye
- Alta, modificación de descripción, y activación/desactivación de Permissions.
### No incluye
- Renombrar una Permission existente (equivale a crear una nueva).
- Desactivación de Permissions de sistema (`isSystemPermission = true`).

## 5. Criterios de aceptación
- Una Permission desactivada deja de contar en cualquier evaluación de autorización de forma inmediata.
- Un intento de desactivar una Permission de sistema es rechazado.

## 6. Restricciones
- Ver `ADR-020` (soft deactivation).

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-031 — Asignación explícita de Roles

## 1. Requisito
Un administrador con el permiso correspondiente debe poder asignar explícitamente un Role a un usuario, quedando registrado con `provenance = GRANTED_EXPLICITLY`, prevaleciendo sobre cualquier derivación futura de AD para ese mismo Role.

## 2. Tipo
Funcional

## 3. Objetivo
Proveer el mecanismo productor de asignaciones `GRANTED_EXPLICITLY`, ya modelado pero sin flujo documentado hasta esta especificación.

## 4. Alcance
### Incluye
- Asignación explícita de un Role a cualquier usuario (`LOCAL` o `ACTIVE_DIRECTORY`).
- Actualización (upgrade) de una asignación `DERIVED_FROM_AD` existente a `GRANTED_EXPLICITLY` cuando corresponda al mismo par `(usuario, Role)`.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Un usuario sin ese Role lo recibe con `provenance = GRANTED_EXPLICITLY`.
- Un usuario que ya lo tenía `DERIVED_FROM_AD` ve su asignación actualizada a `GRANTED_EXPLICITLY`, sin duplicarse.
- Un usuario en `PENDING_ONBOARDING` pasa a `ACTIVE` si la asignación le otorga su primer rol.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-007

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-032 — Revocación explícita de Roles

## 1. Requisito
Un administrador con el permiso correspondiente debe poder revocar una asignación de Role con `provenance = GRANTED_EXPLICITLY`.

## 2. Tipo
Funcional

## 3. Objetivo
Permitir corregir o retirar una asignación explícita sin esperar un ciclo de sincronización AD (que no aplica a asignaciones explícitas).

## 4. Alcance
### Incluye
- Revocación de asignaciones `GRANTED_EXPLICITLY`.
### No incluye
- Revocación directa de asignaciones `DERIVED_FROM_AD` (se gestionan mediante el mapping, `SPEC-AUTH-008`, o se resuelven automáticamente en el próximo login AD).

## 5. Criterios de aceptación
- Revocar el último Role de un usuario lo devuelve a `PENDING_ONBOARDING`.
- Un intento de revocar el Role `MASTER_ADMIN` del único usuario que sostiene la continuidad administrativa es rechazado.
- Un intento de revocar "explícitamente" una asignación que es puramente `DERIVED_FROM_AD` es rechazado con un error que orienta al mecanismo correcto.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-031, REQ-AUTH-033

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`

---

# REQ-AUTH-033 — Continuidad garantizada del rol MASTER_ADMIN

## 1. Requisito
El sistema debe garantizar, en todo momento, la existencia de al menos un `User` `LOCAL` `ACTIVE` con el rol de sistema `MASTER_ADMIN`, rechazando transaccionalmente cualquier operación administrativa de este módulo que dejaría al sistema sin esa garantía.

## 2. Tipo
Funcional

## 3. Objetivo
Formalizar, con un mecanismo concreto y verificable, la continuidad administrativa ya aprobada en `SPEC-AUTH-009`.

## 4. Alcance
### Incluye
- Validación transaccional sobre: deshabilitar, bloquear, deprovisionar o revocar el rol del único usuario `MASTER_ADMIN` activo; desactivar el propio Role `MASTER_ADMIN`.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Ninguna combinación de operaciones de este módulo puede dejar al sistema sin ningún `User` `ACTIVE` con el rol `MASTER_ADMIN`.

## 6. Restricciones
Ninguna adicional.

## 7. Dependencias
- REQ-AUTH-016

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-009`, `SPEC-AUTH-010`

---

# REQ-AUTH-034 — Auditoría de administración de identidad y autorización

## 1. Requisito
Toda operación administrativa sensible sobre usuarios, roles, permisos y asignaciones debe registrarse en Security Audit.

## 2. Tipo
Funcional

## 3. Objetivo
Extender la trazabilidad de seguridad ya exigida (REQ-AUTH-017) a la nueva capacidad de administración.

## 4. Alcance
### Incluye
- Catálogo de eventos definido en `docs/03-architecture/security.md`.
### No incluye
Ninguna exclusión relevante.

## 5. Criterios de aceptación
- Toda alta, modificación, cambio de estado, asignación o revocación queda auditada con actor, sujeto (cuando aplica) y resultado.

## 6. Restricciones
- Nunca se registran contraseñas, tokens ni secretos (REQ-AUTH-023).

## 7. Dependencias
Ninguna.

## 8. Trazabilidad
- Especificaciones relacionadas: `SPEC-AUTH-010`
