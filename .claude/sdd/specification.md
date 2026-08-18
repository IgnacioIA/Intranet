# Especificación

Este documento define qué se entiende por especificación dentro del
proyecto y cuáles son los principios, contenidos y reglas que deben
respetarse al crear, modificar y revisar especificaciones.

Las especificaciones son artefactos de ingeniería y constituyen la
referencia principal para determinar qué comportamiento debe tener el
sistema.

---

# 1. Propósito

Una especificación debe transformar una necesidad o requisito en un
comportamiento suficientemente preciso como para permitir:

- Comprender qué debe hacer el sistema.
- Identificar quién interactúa con él.
- Identificar las reglas que debe respetar.
- Identificar las restricciones relevantes.
- Diseñar una solución.
- Implementar la funcionalidad.
- Diseñar pruebas.
- Verificar el comportamiento.
- Mantener trazabilidad.

La especificación debe reducir la ambigüedad antes de que esta llegue a la
implementación.

---

# 2. La especificación como contrato

Una especificación representa el comportamiento esperado del sistema.

Debe funcionar como un contrato entre:

```text
Necesidad del negocio
        ↓
Especificación
        ↓
Implementación
        ↓
Pruebas

La implementación debe satisfacer la especificación.

Si durante la implementación se descubre que la especificación es
incorrecta o incompleta, no debe modificarse silenciosamente.

Debe iniciarse el proceso correspondiente de análisis y cambio.

3. Especificación no significa implementación

La especificación debe describir:

Qué debe hacer el sistema.
Qué reglas debe respetar.
Qué comportamiento es válido.
Qué comportamiento no es válido.

No debe describir innecesariamente:

Clases concretas.
Métodos concretos.
Variables.
Estructuras internas de código.
Detalles de implementación que puedan cambiar.

Ejemplo incorrecto:

El sistema debe utilizar la clase ComunicadoService
y llamar al método publicarComunicado().

Ejemplo correcto:

El sistema debe permitir publicar un comunicado cuando
el usuario posea los permisos necesarios y se cumplan las
reglas de publicación.

La implementación concreta pertenece al código y a la documentación técnica.

4. Características de una buena especificación

Una especificación debe ser:

Clara.
Precisa.
Consistente.
Verificable.
Trazable.
Comprensible.
Proporcional a la complejidad.
Independiente de detalles innecesarios de implementación.
Compatible con las decisiones arquitectónicas vigentes.

Debe evitar:

Ambigüedad.
Contradicciones.
Suposiciones ocultas.
Requisitos implícitos.
Duplicación innecesaria.
Detalles técnicos prematuros.
5. Unidad de especificación

Una especificación debe representar una capacidad o comportamiento
identificable del sistema.

Ejemplos:

Crear usuario
Asignar rol
Publicar comunicado
Crear recurso
Registrar evento
Consultar cliente

Una especificación no debe ser excesivamente grande.

Si contiene múltiples comportamientos independientes, debe analizarse si
conviene dividirla.

Sin embargo, tampoco debe dividirse artificialmente una funcionalidad que
posee una única regla o comportamiento coherente.

La granularidad debe ser determinada por el dominio y por la necesidad de
mantener trazabilidad.

6. Identificación

Cada especificación relevante debe poseer un identificador único.

El identificador debe permitir relacionarla con otros artefactos.

Ejemplo:

SPEC-USR-001
SPEC-USR-002
SPEC-COM-001
SPEC-REC-001

La convención concreta de nomenclatura será definida en:

.claude/documentation/conventions.md
7. Información mínima

Una especificación debería definir, cuando corresponda:

Identificación
Objetivo
Alcance
Actores
Precondiciones
Postcondiciones
Requisitos relacionados
Caso de uso
Flujo principal
Flujos alternativos
Reglas de negocio
Errores
Casos límite
Criterios de aceptación
Dependencias
Restricciones
Seguridad
Trazabilidad

No todos los elementos son obligatorios para todas las especificaciones.

La documentación debe ser proporcional a la complejidad y riesgo.

8. Objetivo

Debe explicar qué capacidad o comportamiento se quiere obtener.

Ejemplo:

Permitir que un usuario autorizado publique un comunicado
visible para los usuarios correspondientes.

El objetivo debe describir el resultado esperado y no la implementación.

9. Alcance

Debe definir qué incluye la especificación y qué queda fuera.

Ejemplo:

Incluye:
- Creación de comunicados.
- Edición de borradores.
- Publicación.
- Archivado.

No incluye:
- Envío de notificaciones externas.
- Gestión de campañas.

Definir el alcance evita que una especificación crezca indefinidamente.

10. Actores

Debe identificarse quién interactúa con el comportamiento.

Un actor puede ser:

Usuario.
Administrador.
Auditor.
Sistema externo.
Proceso automático.
Otro módulo.

No debe asumirse que todos los usuarios tienen las mismas capacidades.

Cuando existan diferencias de permisos, deben especificarse.

11. Precondiciones

Las precondiciones representan condiciones que deben cumplirse antes de
ejecutar una operación.

Ejemplo:

- El usuario debe estar autenticado.
- El usuario debe poseer permiso de publicación.
- El comunicado debe encontrarse en estado BORRADOR.

Las precondiciones deben poder verificarse.

12. Postcondiciones

Representan condiciones que deben cumplirse después de una operación
exitosa.

Ejemplo:

- El comunicado queda en estado PUBLICADO.
- Se registra la fecha de publicación.
- Se registra el usuario que realizó la operación.

Cuando corresponda, las postcondiciones deben relacionarse con invariantes y
estados del dominio.

13. Requisitos relacionados

Una especificación debe indicar qué requisitos satisface.

Ejemplo:

REQ-COM-001
REQ-AUTH-004

Esto permite mantener trazabilidad entre las necesidades del sistema y su
implementación.

14. Casos de uso

Cuando corresponda, una especificación debe relacionarse con uno o más
casos de uso.

Un caso de uso debe representar una interacción orientada a un objetivo.

Ejemplo:

UC-COM-001
Publicar comunicado

La especificación debe permitir conocer:

Actor.
Objetivo.
Precondiciones.
Flujo principal.
Flujos alternativos.
Errores.
Resultado esperado.
15. Flujo principal

Debe describir el comportamiento esperado cuando todo ocurre normalmente.

Ejemplo:

1. El usuario selecciona un comunicado en estado BORRADOR.
2. El sistema verifica los permisos.
3. El sistema valida las reglas de publicación.
4. El sistema cambia el estado a PUBLICADO.
5. El sistema registra la operación.
6. El sistema confirma la publicación.

El flujo debe describir comportamiento y no detalles internos de código.

16. Flujos alternativos

Deben describirse comportamientos válidos diferentes al flujo principal.

Ejemplo:

Si el comunicado posee una fecha de publicación futura:

1. El sistema acepta la solicitud.
2. El comunicado queda programado.
3. La publicación efectiva se realizará según las reglas definidas.

Los flujos alternativos forman parte del comportamiento especificado.

17. Errores

Los errores esperados deben formar parte de la especificación.

Ejemplos:

Usuario no autenticado
Usuario sin permiso
Entidad inexistente
Estado inválido
Datos inválidos
Regla de negocio incumplida
Conflicto de estado

La especificación debe definir el comportamiento esperado ante errores
relevantes.

No necesariamente debe definir detalles técnicos de cómo se serializa el
error.

18. Reglas de negocio

Las reglas de negocio representan restricciones o comportamientos derivados
del dominio.

Ejemplo:

Un comunicado solamente puede publicarse si se encuentra
en estado BORRADOR.

Las reglas de negocio deben mantenerse separadas de los detalles técnicos.

Una regla de negocio no debería depender innecesariamente de:

Spring.
JPA.
HTTP.
MySQL.
Un framework específico.
19. Entidades y conceptos del dominio

Cuando una especificación dependa de conceptos del dominio, debe
relacionarlos.

Ejemplo:

Comunicado
Usuario
Rol
Permiso
Recurso

La especificación no debe definir arbitrariamente conceptos que contradigan
el modelo de dominio existente.

Cuando un nuevo concepto del dominio sea necesario, debe analizarse mediante
las reglas definidas en:

.claude/ddd/
20. Estados

Cuando una entidad posea estados relevantes para la funcionalidad, deben
identificarse.

Ejemplo:

BORRADOR
PUBLICADO
ARCHIVADO

La especificación debe indicar qué estados participan en el comportamiento.

No debe inventar estados únicamente para facilitar la implementación.

21. Transiciones

Cuando una operación provoque un cambio de estado, debe especificarse.

Ejemplo:

BORRADOR
    ↓
PUBLICADO

Debe quedar claro:

Qué acción produce la transición.
Quién puede ejecutarla.
Qué condiciones deben cumplirse.
Qué transición es válida.
Qué ocurre si la transición no es válida.
22. Invariantes

Cuando existan condiciones que siempre deban cumplirse, deben especificarse
como invariantes.

Ejemplo:

Un comunicado PUBLICADO debe tener una fecha de publicación.

Las invariantes representan restricciones fundamentales del dominio.

No deben confundirse con validaciones puramente técnicas.

23. Criterios de aceptación

Toda funcionalidad relevante debe tener criterios que permitan determinar
objetivamente si fue implementada correctamente.

Los criterios deben ser:

Observables.
Verificables.
Específicos.
Relacionados con el comportamiento esperado.

Ejemplo:

Dado un usuario autenticado con permiso de publicación
y un comunicado en estado BORRADOR,

cuando el usuario publica el comunicado,

entonces el comunicado pasa a estado PUBLICADO
y la operación queda registrada.

Los criterios de aceptación deben servir como base para las pruebas.

24. Casos límite

Deben identificarse situaciones que puedan revelar problemas de diseño.

Ejemplos:

Dos usuarios intentan modificar el mismo recurso.
El recurso desaparece durante la operación.
El usuario pierde permisos.
La entidad cambia de estado.
Una operación se ejecuta dos veces.
El sistema se encuentra temporalmente sin disponibilidad de una dependencia.

Los casos límite relevantes deben ser analizados antes de la implementación.

25. Seguridad

Las especificaciones deben contemplar los aspectos de seguridad relevantes.

Según corresponda deben definirse:

Requisitos de autenticación.
Requisitos de autorización.
Roles.
Permisos.
Acceso a información.
Protección de información sensible.
Auditoría.
Trazabilidad.
Validaciones.
Riesgos de abuso.

La seguridad no debe considerarse exclusivamente una tarea de
implementación.

26. Dependencias

Debe identificarse cuando una especificación dependa de:

Otro módulo.
Otra especificación.
Una entidad.
Un servicio.
Un sistema externo.
Infraestructura.
Una decisión arquitectónica.

Ejemplo:

SPEC-COM-001
depende de:

SPEC-AUTH-001
Autenticación

SPEC-USR-002
Roles y permisos

SPEC-REC-001
Recursos

Esto permite identificar el orden necesario de implementación.

27. Restricciones

Una especificación puede tener restricciones técnicas o de negocio.

Ejemplos:

- El sistema debe utilizar la base de datos existente.
- La información debe permanecer dentro de la infraestructura
  de la empresa.
- El comportamiento debe ser compatible con la arquitectura definida.

Las restricciones deben distinguirse de las decisiones.

Una restricción puede provenir de:

Negocio.
Seguridad.
Infraestructura.
Regulación.
Arquitectura.
Sistemas existentes.
28. Suposiciones

Las suposiciones deben identificarse explícitamente.

Ejemplo:

SUP-001

Se asume que todos los usuarios internos poseen una cuenta
activa antes de acceder al módulo.

Una suposición importante debe convertirse en decisión o requisito cuando
corresponda.

No debe permanecer indefinidamente como una verdad implícita.

29. GAPs

Cuando exista información necesaria que todavía no fue definida, debe
registrarse como GAP.

Ejemplo:

GAP-001

No está definido si un usuario puede tener múltiples roles.

Los GAPs deben:

Identificarse.
Evaluarse.
Resolver mediante una decisión.
Actualizar la especificación.

Claude no debe ocultar un GAP mediante una suposición cuando la decisión
tenga impacto relevante.

30. Separación entre requisito y especificación

Un requisito expresa una necesidad o condición que el sistema debe cumplir.

Una especificación desarrolla cómo se manifiesta ese requisito en el
comportamiento esperado.

Ejemplo:

Requisito:

Los usuarios autorizados deben poder publicar comunicados.

Especificación:

Un usuario autenticado con el permiso correspondiente
puede cambiar un comunicado de BORRADOR a PUBLICADO,
siempre que se cumplan las reglas de publicación.

La especificación debe mantener la relación con el requisito original.

31. Separación entre especificación y diseño

La especificación define el comportamiento.

El diseño define cómo construirlo.

Ejemplo:

ESPECIFICACIÓN

Un usuario autorizado puede publicar un comunicado.

Diseño:

Controller
Application Service
Domain Model
Repository
Database

El diseño puede evolucionar mientras mantenga el comportamiento especificado.

32. Separación entre especificación y código

El código implementa la especificación.

La especificación no debe convertirse en una descripción línea por línea del
código.

Si la implementación cambia internamente y el comportamiento no cambia, la
especificación normalmente no necesita modificarse.

Si cambia el comportamiento, la especificación debe revisarse.

33. Especificaciones técnicas

No todo debe expresarse como comportamiento de negocio.

Pueden existir especificaciones técnicas para:

APIs.
Integraciones.
Persistencia.
Infraestructura.
Seguridad.
Procesos automáticos.

Estas especificaciones deben utilizar el mismo principio:

Necesidad
   ↓
Comportamiento esperado
   ↓
Criterios verificables

Los detalles concretos de implementación deben mantenerse separados cuando
sea posible.

34. Granularidad

La especificación debe ser suficientemente pequeña para poder:

Revisarse.
Implementarse.
Probarse.
Trazarse.

Pero suficientemente grande para representar una capacidad coherente.

No debe existir una especificación por cada método o clase.

Tampoco debe existir una única especificación gigantesca para todo un
módulo.

35. Composición de especificaciones

Una funcionalidad compleja puede depender de varias especificaciones.

Ejemplo:

Módulo Comunicados

SPEC-COM-001
Crear comunicado

SPEC-COM-002
Editar comunicado

SPEC-COM-003
Publicar comunicado

SPEC-COM-004
Archivar comunicado

Las especificaciones pueden compartir:

Entidades.
Reglas.
Requisitos.
Decisiones.

Estas relaciones deben documentarse cuando sean relevantes.

36. Estado de una especificación

Toda especificación debe poseer un estado definido por:

.claude/sdd/lifecycle.md

Por ejemplo:

spec_status: DRAFT

o:

spec_status: APPROVED

El estado debe representar realmente la situación del artefacto.

No debe modificarse únicamente para hacer coincidir la documentación con el
estado del código.

37. Implementación y especificación

Una implementación debe poder responder:

¿Qué especificación estoy implementando?

Una especificación debe poder responder:

¿Qué implementación la satisface?

Cuando sea necesario, esta relación debe mantenerse mediante la
trazabilidad.

38. Pruebas y especificación

Las pruebas deben derivarse del comportamiento especificado.

La relación conceptual debe ser:

Criterio de aceptación
        ↓
Caso de prueba
        ↓
Resultado

Una prueba no debe considerarse suficiente únicamente porque exista.

Debe verificar un comportamiento definido.

39. Cambios de especificación

Una especificación aprobada no debe modificarse arbitrariamente.

Cuando cambie el comportamiento esperado debe utilizarse el proceso definido
en:

.claude/sdd/change-management.md

El cambio debe analizar:

Motivo.
Impacto.
Dependencias.
Implementación afectada.
Pruebas afectadas.
Trazabilidad afectada.
40. Calidad de una especificación

Antes de considerar una especificación preparada para implementación,
Claude debe comprobar:

[ ] El objetivo está definido.
[ ] El alcance está definido.
[ ] Los actores están identificados.
[ ] Los requisitos relacionados están identificados.
[ ] Las reglas de negocio están identificadas.
[ ] Los casos de uso están definidos cuando corresponda.
[ ] Los estados están definidos cuando corresponda.
[ ] Las transiciones están definidas cuando corresponda.
[ ] Las invariantes están identificadas cuando corresponda.
[ ] Los errores relevantes están definidos.
[ ] Los casos límite relevantes fueron considerados.
[ ] Los criterios de aceptación son verificables.
[ ] Las dependencias están identificadas.
[ ] Las restricciones están identificadas.
[ ] Los GAPs relevantes fueron resueltos.
[ ] Las decisiones necesarias están aprobadas.
[ ] Los requisitos de seguridad fueron considerados.
[ ] La especificación es trazable.

No todos los elementos serán obligatorios en todas las especificaciones.

41. Regla contra la sobreespecificación

Claude debe evitar agregar detalles únicamente para hacer que la
especificación parezca más completa.

Una especificación no mejora por tener más texto.

Debe contener la información necesaria para:

Comprender.
Decidir.
Implementar.
Probar.
Verificar.

La documentación innecesaria aumenta el coste de mantenimiento.

42. Regla contra la subespecificación

Claude tampoco debe considerar suficiente una especificación que deje
implícitas decisiones relevantes.

Ejemplo insuficiente:

El administrador puede gestionar usuarios.

Debe determinarse qué significa "gestionar":

Crear.
Modificar.
Desactivar.
Eliminar.
Asignar roles.
Consultar.
Restablecer credenciales.

Cuando la información no esté definida, debe identificarse como GAP o
decisión pendiente.

43. Evolución de la especificación

La especificación debe evolucionar junto con el sistema.

El objetivo es mantener:

Necesidad
   ↕
Requisito
   ↕
Especificación
   ↕
Implementación
   ↕
Pruebas

Si una de estas relaciones se rompe, debe detectarse y corregirse.

44. Principio general

Una buena especificación debe permitir que una persona que no participó en
la implementación pueda comprender:

Qué hace la funcionalidad.
Para quién existe.
Qué reglas debe cumplir.
Qué situaciones contempla.
Qué situaciones rechaza.
Cómo se verifica.
Por qué ciertas decisiones existen.

La especificación debe preservar conocimiento del sistema que de otro modo
quedaría únicamente en el código o en la memoria de los desarrolladores.