# Trazabilidad

Este documento define cómo se relacionan los diferentes artefactos de
ingeniería del proyecto durante el ciclo de vida del software.

La trazabilidad permite conocer la relación entre:

```text
Necesidad
   ↓
Requisito
   ↓
Especificación
   ↓
Caso de uso
   ↓
Modelo de dominio
   ↓
Decisión
   ↓
Implementación
   ↓
Prueba
   ↓
Verificación

El objetivo no es documentar cada línea de código.

El objetivo es poder reconstruir las razones, decisiones y relaciones que
explican el comportamiento del sistema.

1. Propósito

La trazabilidad debe permitir responder preguntas como:

¿Por qué existe esta funcionalidad?
¿Qué requisito satisface?
¿Qué especificación define su comportamiento?
¿Qué caso de uso la utiliza?
¿Qué entidades del dominio intervienen?
¿Qué reglas de negocio la condicionan?
¿Qué decisión arquitectónica la justifica?
¿Qué código la implementa?
¿Qué pruebas verifican su comportamiento?
¿Qué funcionalidades serán afectadas si cambia este requisito?
2. Principio de trazabilidad bidireccional

La trazabilidad debe funcionar en ambos sentidos.

Hacia adelante

Permite comenzar desde una necesidad y llegar a su implementación.

Requisito
   ↓
Especificación
   ↓
Caso de uso
   ↓
Implementación
   ↓
Prueba
Hacia atrás

Permite comenzar desde una implementación y conocer su origen.

Código
   ↓
Especificación
   ↓
Requisito
   ↓
Necesidad

Ambas direcciones son importantes.

3. Cadena principal

La cadena conceptual principal del proyecto será:

REQ
 ↓
SPEC
 ↓
UC
 ↓
DOMAIN
 ↓
CODE
 ↓
TEST

Donde:

REQ    = Requisito
SPEC   = Especificación
UC     = Caso de uso
DOMAIN = Modelo de dominio
CODE   = Implementación
TEST   = Prueba

No todos los elementos deben existir para todas las funcionalidades.

La relación debe utilizarse cuando aporte valor.

4. Artefactos trazables

Los principales artefactos que pueden participar de la trazabilidad son:

Requisitos
Especificaciones
Casos de uso
Entidades
Value Objects
Agregados
Reglas de negocio
Invariantes
Estados
Transiciones
Decisiones arquitectónicas
Decisiones de seguridad
Contratos de API
Implementaciones
Pruebas
Cambios
5. Identificadores

Los artefactos relevantes deben poseer identificadores estables.

Ejemplos:

REQ-AUTH-001
SPEC-AUTH-001
UC-AUTH-001

REQ-USR-001
SPEC-USR-001
UC-USR-001

REQ-COM-001
SPEC-COM-001
UC-COM-001

ADR-001

TEST-AUTH-001

Los identificadores concretos y sus convenciones serán definidos en:

.claude/documentation/conventions.md
6. Requisitos → Especificaciones

Toda especificación relevante debe indicar qué requisitos satisface.

Ejemplo:

REQ-COM-001
Los usuarios autorizados deben poder publicar comunicados.

        ↓

SPEC-COM-003
Publicar comunicado

Una especificación puede satisfacer múltiples requisitos.

Un requisito puede estar satisfecho por múltiples especificaciones.

La relación no debe asumirse como uno a uno.

7. Especificaciones → Casos de uso

Cuando corresponda, una especificación debe relacionarse con uno o más
casos de uso.

Ejemplo:

SPEC-COM-003
Publicar comunicado

        ↓

UC-COM-003
Publicar comunicado

El caso de uso debe representar la interacción necesaria para obtener el
comportamiento definido por la especificación.

8. Casos de uso → Dominio

Cuando un caso de uso interactúe con conceptos del dominio, deben
identificarse las entidades, agregados, value objects o reglas involucradas.

Ejemplo:

UC-COM-003
Publicar comunicado

        ↓

Comunicado
Usuario
Rol
Permiso

Esto permite conocer qué parte del dominio participa en el comportamiento.

9. Dominio → Implementación

La implementación debe poder relacionarse con los conceptos del dominio.

Ejemplo conceptual:

Comunicado
    ↓
Comunicado.java

Publicar comunicado
    ↓
Servicio de aplicación correspondiente

Regla de publicación
    ↓
Regla del dominio

La trazabilidad no requiere registrar cada método.

Debe identificar los componentes relevantes.

10. Implementación → Pruebas

Toda funcionalidad relevante debe poder relacionarse con las pruebas que
verifican su comportamiento.

Ejemplo:

SPEC-COM-003
        ↓
TEST-COM-003

Cuando corresponda, una especificación puede tener múltiples pruebas:

SPEC-COM-003
      ↓
      ├── TEST-COM-003-01
      ├── TEST-COM-003-02
      └── TEST-COM-003-03
11. Requisitos → Pruebas

También debe ser posible recorrer la relación directamente:

REQ-COM-001
     ↓
SPEC-COM-003
     ↓
TEST-COM-003

Esto permite determinar si un requisito tiene evidencia de verificación.

12. Criterios de aceptación → Pruebas

Los criterios de aceptación deben poder relacionarse con pruebas.

Ejemplo:

AC-COM-003-01

Dado un usuario autorizado,
cuando publica un comunicado válido,
entonces el comunicado queda publicado.

        ↓

TEST-COM-003-01

Esto permite que las pruebas tengan un origen explícito.

13. Decisiones → Artefactos

Las decisiones importantes deben poder relacionarse con los artefactos que
las utilizan.

Ejemplo:

ADR-005
Un usuario puede tener múltiples roles.

        ↓

SPEC-AUTH-003
SPEC-USR-004
Modelo Usuario
Modelo Rol

Esto permite conocer qué partes del sistema podrían verse afectadas si la
decisión cambia.

14. Cambios → Trazabilidad

Un cambio relevante debe relacionarse con los artefactos afectados.

Ejemplo:

CR-COM-003
Agregar fecha de expiración

        ↓
SPEC-COM-001
SPEC-COM-003
Entidad Comunicado
Base de datos
TEST-COM-003

Esto permite conocer el impacto real del cambio.

15. Trazabilidad de seguridad

Los requisitos de seguridad también deben ser trazables.

Ejemplo:

REQ-AUTH-001
El sistema debe autenticar a los usuarios.

        ↓

SPEC-AUTH-001
Autenticar usuario.

        ↓

Implementación

        ↓

TEST-AUTH-001

Para requisitos de seguridad críticos debe existir evidencia suficiente de
su verificación.

16. Trazabilidad de permisos

Los permisos deben poder relacionarse con los comportamientos que protegen.

Ejemplo:

PERM-COM-PUBLICAR

        ↓

SPEC-COM-003
Publicar comunicado

        ↓

UC-COM-003

Esto permite responder:

¿Por qué existe este permiso?

y:

¿Qué funcionalidades protege?

17. Trazabilidad de auditoría

Las operaciones que requieran auditoría deben poder relacionarse con el
comportamiento que origina el evento.

Ejemplo:

UC-USR-003
Desactivar usuario

        ↓

Evento de auditoría:
USUARIO_DESACTIVADO

La auditoría no debe registrar eventos arbitrarios sin relación con el
comportamiento del sistema.

18. Trazabilidad de logs

Los logs técnicos deben diferenciarse de los eventos de auditoría.

Un log puede utilizarse para:

Diagnóstico.
Depuración.
Errores.
Rendimiento.
Operación.

Una auditoría representa un hecho relevante del sistema desde el punto de
vista funcional, administrativo o de seguridad.

La trazabilidad debe evitar confundir ambos conceptos.

19. Trazabilidad de estados

Cuando una especificación utilice estados, la relación debe quedar clara.

Ejemplo:

SPEC-COM-003
Publicar comunicado

        ↓

BORRADOR → PUBLICADO

Las transiciones pueden estar relacionadas con:

Casos de uso.
Reglas de negocio.
Permisos.
Pruebas.
20. Trazabilidad de invariantes

Las invariantes relevantes deben poder rastrearse hasta las funcionalidades
que dependen de ellas.

Ejemplo:

INV-COM-001
Todo comunicado PUBLICADO debe tener fecha de publicación.

        ↓

SPEC-COM-003
Publicar comunicado

        ↓

TEST-COM-003-02

Esto permite verificar que las reglas fundamentales del dominio realmente
se están protegiendo.

21. Trazabilidad entre módulos

Las dependencias entre módulos también deben poder identificarse.

Ejemplo:

Módulo Comunicados
        ↓
Módulo Usuarios
        ↓
Módulo Autenticación

Otro ejemplo:

Comunicados
    ↓
Recursos
    ↓
Almacenamiento

La trazabilidad ayuda a determinar el impacto de modificaciones entre
módulos.

22. Trazabilidad y orden de implementación

La trazabilidad también permite identificar dependencias de implementación.

Ejemplo:

Autenticación
      ↓
Usuarios
      ↓
Roles y permisos
      ↓
Comunicados
      ↓
Notificaciones

Esto no significa que la dependencia funcional siempre implique una
dependencia técnica directa.

Debe analizarse cada caso.

23. Matriz de trazabilidad

El proyecto puede utilizar una matriz para representar las relaciones
principales.

Ejemplo:

Requisito	Especificación	Caso de uso	Dominio	Prueba
REQ-COM-001	SPEC-COM-003	UC-COM-003	Comunicado	TEST-COM-003
REQ-AUTH-001	SPEC-AUTH-001	UC-AUTH-001	Usuario	TEST-AUTH-001

La matriz debe mantenerse simple.

No debe convertirse en una tabla gigantesca que duplique toda la
documentación.

24. Trazabilidad distribuida

La trazabilidad no necesariamente debe estar almacenada en un único archivo.

Puede estar distribuida entre:

Requisitos
Especificaciones
ADR
Documentación de dominio
Contratos
Pruebas

Cada artefacto debe contener referencias suficientes para reconstruir sus
relaciones.

25. No duplicar información

La trazabilidad no debe provocar duplicación innecesaria.

No se debe copiar el contenido completo de una especificación dentro de un
requisito, una prueba o un ADR.

Debe utilizarse una referencia.

Ejemplo correcto:

Relacionado con:
SPEC-COM-003

Ejemplo incorrecto:

[Copiar aquí toda la especificación]
26. Trazabilidad mínima

No todas las funcionalidades requieren el mismo nivel de trazabilidad.

Una funcionalidad sencilla puede requerir:

REQ
 ↓
SPEC
 ↓
TEST

Una funcionalidad compleja puede requerir:

REQ
 ↓
SPEC
 ↓
UC
 ↓
DOMAIN
 ↓
ADR
 ↓
CODE
 ↓
TEST

El nivel debe ser proporcional a:

Complejidad.
Riesgo.
Impacto.
Seguridad.
Criticidad.
27. Trazabilidad obligatoria

Debe existir trazabilidad explícita para:

Requisitos críticos.
Reglas de negocio importantes.
Seguridad.
Autenticación.
Autorización.
Auditoría.
Integridad de datos.
Cambios arquitectónicos.
Contratos externos.
Funcionalidades críticas.
28. Trazabilidad opcional

Puede utilizarse un nivel reducido para:

Refactorizaciones internas.
Correcciones de estilo.
Cambios puramente internos.
Mejoras que no alteran comportamiento.

La ausencia de una referencia no debe considerarse automáticamente un
error.

29. Trazabilidad rota

Se considera que existe una ruptura cuando:

Una especificación no tiene requisito relacionado cuando debería tenerlo.
Un requisito no tiene implementación.
Una implementación no puede relacionarse con ninguna especificación.
Una prueba no puede relacionarse con el comportamiento que verifica.
Una decisión contradice la documentación vigente.
Una especificación fue modificada pero las pruebas no fueron revisadas.
Un cambio afecta un artefacto que no fue actualizado.

Las rupturas deben ser identificadas.

30. Trazabilidad huérfana

Se considera un artefacto huérfano cuando existe sin una relación clara con
el resto del sistema.

Ejemplos:

SPEC-USR-009

sin requisito, caso de uso ni módulo identificable.

O:

TEST-COM-015

sin comportamiento que justifique su existencia.

Los artefactos huérfanos deben investigarse.

31. Código sin especificación

No todo código requiere una especificación individual.

Sin embargo, código que implementa comportamiento relevante del negocio debe
poder relacionarse con una especificación.

Ejemplo:

ComunicadoService
        ↓
SPEC-COM-003

Las utilidades técnicas internas pueden quedar fuera de esta relación si no
representan comportamiento del negocio.

32. Especificación sin implementación

Una especificación puede existir sin implementación.

Ejemplo:

SPEC-NOT-004
Programar notificaciones

Estado:
APPROVED

Implementación:
Pendiente

Esto no constituye una ruptura de trazabilidad.

Representa una funcionalidad definida pero todavía no implementada.

33. Implementación sin especificación

Si aparece una funcionalidad relevante que no posee especificación, Claude
debe identificarla.

Debe determinar si:

La especificación existe pero no fue relacionada.
La documentación está desactualizada.
La funcionalidad fue implementada fuera del alcance.
Existe una necesidad de crear una nueva especificación.

No debe inventarse retrospectivamente una especificación sin analizar el
comportamiento real.

34. Prueba sin especificación

Una prueba sin relación con una especificación debe investigarse.

Puede ser:

Una prueba técnica.
Una prueba de infraestructura.
Una prueba de regresión.
Una prueba relacionada con una especificación que no fue enlazada.

No debe eliminarse automáticamente.

35. Cambios y trazabilidad

Después de un cambio significativo debe revisarse:

Requisitos
↓
Especificaciones
↓
Dominio
↓
Arquitectura
↓
Implementación
↓
Pruebas

No basta con modificar únicamente el artefacto donde se detectó el cambio.

36. Trazabilidad durante la implementación

Claude debe mantener la relación entre la tarea actual y los artefactos que
la originaron.

Antes de implementar una funcionalidad relevante debe poder identificar:

¿Qué requisito estoy satisfaciendo?

¿Qué especificación estoy implementando?

¿Qué caso de uso estoy implementando?

¿Qué reglas del dominio debo respetar?

¿Qué decisiones arquitectónicas debo respetar?

¿Qué pruebas deben existir?

Si estas respuestas no pueden obtenerse, Claude debe detenerse y solicitar
o proponer la información faltante.

37. Trazabilidad durante la revisión

Durante una revisión debe verificarse:

¿El código implementa la especificación?
¿La especificación satisface el requisito?
¿Las pruebas verifican el comportamiento?
¿Las decisiones siguen siendo válidas?
¿La documentación representa el código actual?

La revisión no debe limitarse al código.

38. Trazabilidad después de la implementación

Una vez implementada una funcionalidad:

Código
 ↓
Pruebas
 ↓
Especificación
 ↓
Requisito

debe poder recorrerse la cadena en sentido inverso.

Si no es posible, debe evaluarse una ruptura de trazabilidad.

39. Trazabilidad y Git

Git representa el historial de cambios del código y de los archivos.

No reemplaza la trazabilidad conceptual.

Git permite responder:

¿Qué cambió?

La trazabilidad debe permitir responder:

¿Por qué cambió?

Ambos mecanismos deben complementarse.

40. Trazabilidad y ADR

Los ADR registran decisiones arquitectónicas.

La trazabilidad debe permitir relacionar:

ADR
 ↓
Especificaciones afectadas
 ↓
Implementación

Esto permite conocer qué partes del sistema dependen de una decisión
arquitectónica.

41. Trazabilidad y documentación histórica

La documentación histórica debe conservarse cuando aporte contexto.

Sin embargo, la documentación vigente debe ser identificable claramente.

No debe obligarse a Claude o a un desarrollador a reconstruir el
comportamiento actual leyendo todo el historial.

La documentación debe distinguir:

Estado actual

de:

Historial de evolución
42. Validación de trazabilidad

Antes de considerar una funcionalidad importante como VERIFIED, Claude debe
comprobar:

[ ] Existe requisito relacionado.
[ ] Existe especificación.
[ ] El comportamiento está definido.
[ ] Las reglas relevantes están identificadas.
[ ] Las dependencias están identificadas.
[ ] Las decisiones relevantes están relacionadas.
[ ] La implementación puede identificarse.
[ ] Las pruebas están relacionadas.
[ ] Los criterios de aceptación fueron verificados.
[ ] La documentación está actualizada.

No todos los puntos serán aplicables a todas las funcionalidades.

43. Responsabilidad de Claude

Claude debe:

Crear referencias de trazabilidad cuando corresponda.
Detectar relaciones faltantes.
Detectar referencias inconsistentes.
Identificar artefactos huérfanos.
Identificar implementaciones sin especificación.
Identificar especificaciones sin implementación cuando corresponda.
Mantener relaciones entre requisitos, especificaciones y pruebas.
Revisar la trazabilidad después de cambios relevantes.
Informar rupturas de trazabilidad.
No inventar relaciones que no estén justificadas.
44. Responsabilidad del responsable del proyecto

El responsable del proyecto debe:

Validar relaciones importantes.
Resolver ambigüedades.
Aprobar decisiones relevantes.
Determinar el nivel de trazabilidad necesario.
Resolver conflictos entre requisitos.
Validar cambios de alcance.
45. Principio general

La trazabilidad no existe para generar burocracia.

Existe para preservar el razonamiento del sistema.

El objetivo final es poder recorrer:

¿Por qué existe?
      ↓
Requisito
      ↓
¿Qué debe hacer?
      ↓
Especificación
      ↓
¿Cómo interactúa?
      ↓
Caso de uso
      ↓
¿Qué conceptos utiliza?
      ↓
Dominio
      ↓
¿Por qué fue diseñado así?
      ↓
Decisión / Arquitectura
      ↓
¿Dónde está implementado?
      ↓
Código
      ↓
¿Cómo sabemos que funciona?
      ↓
Pruebas

Una trazabilidad útil permite comprender no solamente qué hace el sistema,
sino también por qué fue construido de esa manera.