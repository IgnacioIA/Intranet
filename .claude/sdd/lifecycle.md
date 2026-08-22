# Ciclo de Vida de las Especificaciones

Este documento define el ciclo de vida de las especificaciones utilizadas
durante el desarrollo del proyecto mediante el enfoque
Spec-Driven Development (SDD).

Una especificación representa el comportamiento esperado de una parte del
sistema y sirve como referencia para su análisis, implementación y
verificación.

El estado de una especificación debe reflejar su situación real dentro del
proceso de desarrollo.

---

# 1. Principio general

Una especificación no debe considerarse simplemente como un documento.

Es un artefacto de ingeniería que evoluciona durante el ciclo de vida del
software.

Su ciclo general es:

```text
DRAFT
  ↓
IN_REVIEW
  ↓
APPROVED
  ↓
IMPLEMENTING
  ↓
IMPLEMENTED
  ↓
VERIFIED

Sin embargo, una especificación también puede:

DRAFT
  ↓
REJECTED

o:

APPROVED
  ↓
SUPERSEDED

o:

VERIFIED
  ↓
CHANGE_REQUESTED
  ↓
IN_REVIEW

Los estados representan el estado de la especificación, no necesariamente
el estado del código.

2. Estados de una especificación

Una especificación puede encontrarse en los siguientes estados:

DRAFT
IN_REVIEW
APPROVED
IMPLEMENTING
IMPLEMENTED
VERIFIED
REJECTED
SUPERSEDED
DEPRECATED

No todos los proyectos o funcionalidades utilizarán necesariamente todos
los estados.

La utilización debe ser proporcional a la complejidad y relevancia del
artefacto.

3. DRAFT

Representa una especificación en construcción.

Características:

Puede contener información incompleta.
Puede contener preguntas abiertas.
Puede contener GAPs.
Puede contener decisiones pendientes.
Puede cambiar significativamente.
No debe utilizarse como contrato definitivo de implementación.

Una especificación DRAFT puede ser creada durante la fase de análisis.

Ejemplo:

SPEC-COM-001

Estado:
DRAFT

Objetivo:
Definir el comportamiento del módulo de comunicados.

Pendientes:
- Definir quién puede publicar.
- Definir si puede editarse un comunicado publicado.
- Definir expiración.
4. IN_REVIEW

Representa una especificación preparada para revisión.

Al pasar a IN_REVIEW debe:

Tener un objetivo claro.
Tener un alcance definido.
Identificar actores relevantes.
Identificar requisitos relevantes.
Identificar reglas de negocio conocidas.
Identificar dependencias.
Identificar decisiones importantes.
Identificar GAPs pendientes.

Una especificación no debería pasar a revisión si todavía es
conceptualmente incomprensible.

5. APPROVED

Representa una especificación validada.

Una especificación APPROVED:

Representa decisiones aceptadas.
Tiene suficiente precisión para implementar.
Tiene criterios de aceptación cuando corresponda.
No contiene decisiones críticas sin resolver.
Puede utilizarse como referencia para la implementación.

Cuando la especificación defina una capacidad expuesta mediante una API,
"tener suficiente precisión para implementar" incluye que el API Contract
correspondiente esté suficientemente definido. Esto no introduce un nuevo
estado: es una aclaración del mismo criterio de precisión ya exigido para
aprobar cualquier especificación.

La aprobación no significa que el código ya exista.

Significa que el comportamiento esperado fue suficientemente definido y
validado.

6. IMPLEMENTING

Representa una especificación que está siendo implementada.

Este estado indica que existe una relación activa entre la especificación
y cambios en el código.

Durante este estado:

La implementación debe seguir la especificación.
Los cambios relevantes deben mantenerse trazables.
Las decisiones nuevas deben registrarse.
Las desviaciones deben ser identificadas.
Las pruebas deben desarrollarse junto con la implementación.

Una especificación no debe modificarse silenciosamente para justificar una
implementación existente.

7. IMPLEMENTED

Representa una especificación cuya implementación fue completada.

Esto significa que:

El código correspondiente fue desarrollado.
Las tareas principales de implementación fueron completadas.
La funcionalidad está presente en el sistema.

Sin embargo, IMPLEMENTED no significa necesariamente que la funcionalidad
haya sido completamente verificada.

Por eso existe posteriormente el estado VERIFIED.

8. VERIFIED

Representa una especificación cuya implementación fue verificada.

La verificación debe considerar, cuando corresponda:

Requisitos.
Criterios de aceptación.
Reglas de negocio.
Invariantes.
Estados.
Transiciones.
Seguridad.
Pruebas.
Integración.
Arquitectura.
Conformidad con el API Contract, cuando corresponda.

Una especificación puede considerarse VERIFIED cuando exista evidencia
suficiente de que la implementación cumple el comportamiento especificado.

9. REJECTED

Representa una especificación o propuesta que fue evaluada y rechazada.

Una especificación REJECTED:

No debe implementarse.
Debe conservarse cuando exista valor histórico.
Puede incluir el motivo del rechazo.
Puede ser reemplazada posteriormente por otra propuesta.

Ejemplo:

SPEC-AUTH-003

Estado:
REJECTED

Motivo:
Se descartó la utilización de autenticación basada exclusivamente
en sesiones locales debido a los requisitos de integración futura.

El rechazo no debe eliminar automáticamente el documento.

10. SUPERSEDED

Representa una especificación que fue reemplazada por otra.

Ejemplo:

SPEC-USR-001
Estado: SUPERSEDED

Reemplazada por:
SPEC-USR-002

La especificación anterior debe conservarse cuando sea relevante para
comprender la evolución del sistema.

La nueva especificación representa el comportamiento vigente.

11. DEPRECATED

Representa una especificación que describe una funcionalidad o comportamiento
que sigue existiendo o puede existir en el sistema, pero que se encuentra
en proceso de retiro.

Debe utilizarse cuando:

Una funcionalidad será eliminada.
Un comportamiento será reemplazado.
Un contrato dejará de utilizarse.
Una capacidad será migrada hacia otra solución.

Debe indicar, cuando corresponda:

Motivo.
Reemplazo.
Impacto.
Condiciones para su eliminación.
12. Cambio de estado

Los cambios de estado deben ser explícitos.

Flujo normal:

DRAFT
  ↓
IN_REVIEW
  ↓
APPROVED
  ↓
IMPLEMENTING
  ↓
IMPLEMENTED
  ↓
VERIFIED

Flujo de rechazo:

DRAFT
  ↓
IN_REVIEW
  ↓
REJECTED

Flujo de modificación posterior:

VERIFIED
  ↓
CHANGE_REQUESTED
  ↓
DRAFT
  ↓
IN_REVIEW
  ↓
APPROVED

CHANGE_REQUESTED representa una solicitud de modificación y no
necesariamente un estado permanente de la especificación.

Puede utilizarse como estado de transición cuando el proceso de cambio
requiera identificar explícitamente que una especificación verificada dejó
de representar completamente el comportamiento deseado.

13. Reglas para las transiciones

No todas las transiciones son válidas.

DRAFT → IN_REVIEW

Permitida cuando:

La especificación tiene suficiente contenido.
Los problemas críticos fueron identificados.
Las decisiones necesarias están documentadas.
Los GAPs restantes no impiden la revisión.
IN_REVIEW → APPROVED

Permitida cuando:

La especificación fue revisada.
Las decisiones relevantes fueron aceptadas.
No existen contradicciones críticas.
Los criterios de aceptación son suficientes.
El alcance está definido.
IN_REVIEW → REJECTED

Permitida cuando:

La propuesta no será implementada.
Existe una razón para conservar el registro del análisis.
APPROVED → IMPLEMENTING

Permitida cuando comienza la implementación.

No debe iniciarse una implementación relevante basándose únicamente en una
especificación DRAFT.

IMPLEMENTING → IMPLEMENTED

Permitida cuando:

La implementación correspondiente fue completada.
Los cambios previstos fueron realizados.
No existen tareas de implementación conocidas que impidan considerar la
funcionalidad implementada.
IMPLEMENTED → VERIFIED

Permitida cuando existe evidencia suficiente de que la implementación
cumple la especificación.

VERIFIED → CHANGE_REQUESTED

Puede producirse cuando:

Cambia un requisito.
Se detecta un error en la especificación.
Se detecta un comportamiento incorrecto.
Aparece una nueva necesidad.
Se descubre una restricción previamente desconocida.
CHANGE_REQUESTED → DRAFT

La especificación vuelve a una fase de modificación.

Debe analizarse el impacto del cambio antes de modificarla.

14. No saltar estados sin justificación

El proceso normal no debe saltarse etapas arbitrariamente.

Por ejemplo:

DRAFT
   ↓
IMPLEMENTING

no debe ser el flujo habitual.

La implementación debe basarse en una especificación suficientemente
definida.

Sin embargo, pueden existir excepciones para:

Correcciones urgentes.
Incidentes.
Cambios operativos.
Parches de seguridad.
Correcciones de producción.

En estos casos debe registrarse la excepción y posteriormente actualizarse
la documentación correspondiente.

15. Estado de especificación versus estado de implementación

Estos conceptos deben mantenerse separados.

Una especificación puede estar:

APPROVED

mientras el código todavía no existe.

También puede ocurrir:

IMPLEMENTED

pero todavía no:

VERIFIED

Esto permite diferenciar:

"Sabemos qué debemos construir."

de:

"Ya lo construimos."

y de:

"Comprobamos que funciona como fue especificado."

16. Estado de documentación versus estado del software

La existencia de código no garantiza que la documentación sea correcta.

Puede existir:

Código:
IMPLEMENTADO

Especificación:
DRAFT

Esto representa una inconsistencia que debe identificarse.

La documentación y la implementación deben converger hacia un estado
consistente.

17. Modificación de una especificación aprobada

Una especificación APPROVED no debe modificarse directamente para introducir
cambios de alcance sin analizar previamente el impacto.

El proceso debe ser:

Especificación aprobada
        ↓
Solicitud de cambio
        ↓
Análisis de impacto
        ↓
Decisión
        ↓
Nueva versión / revisión
        ↓
Revisión
        ↓
Aprobación

La modificación debe conservar la trazabilidad necesaria.

Los detalles del proceso de cambios se definen en:

.claude/sdd/change-management.md
18. Versionado de especificaciones

Las especificaciones relevantes deben poder diferenciar sus versiones.

El mecanismo concreto de versionado será definido por las convenciones de
documentación.

Como principio:

Versión 1
   ↓
Cambio aprobado
   ↓
Versión 2

No debe utilizarse el versionado para esconder cambios de comportamiento.

Cada cambio relevante debe poder rastrearse hasta la decisión que lo
originó.

19. Estado de especificaciones de un módulo

Un módulo puede contener múltiples especificaciones.

Por ejemplo:

Módulo Usuarios

SPEC-USR-001
Crear usuario

SPEC-USR-002
Modificar usuario

SPEC-USR-003
Asignar rol

SPEC-USR-004
Desactivar usuario

Cada especificación posee su propio ciclo de vida.

El estado general del módulo no debe inferirse simplemente de una única
especificación.

20. Estados de módulos y estados de especificaciones

El proyecto puede utilizar posteriormente estados específicos para módulos.

Por ejemplo:

Módulo
   ↓
PLANIFICADO
   ↓
EN_DESARROLLO
   ↓
IMPLEMENTADO
   ↓
VERIFICADO
   ↓
OPERATIVO

Estos estados son independientes de los estados de las especificaciones.

Una especificación puede estar VERIFIED mientras el módulo todavía tenga
otras especificaciones pendientes.

21. Especificaciones incompletas

Una especificación puede contener elementos pendientes.

Estos deben identificarse explícitamente.

Ejemplo:

GAP-001

No está definido si los administradores pueden eliminar
definitivamente un comunicado publicado.

Un GAP no debe resolverse mediante una suposición silenciosa si afecta una
decisión relevante.

22. Requisitos que cambian durante la implementación

Si durante la implementación se descubre que el requisito original cambió,
no debe modificarse la especificación silenciosamente.

Debe seguirse:

Cambio detectado
      ↓
Análisis
      ↓
Impacto
      ↓
Decisión
      ↓
Actualización de especificación
      ↓
Actualización de implementación
      ↓
Pruebas

Esto evita que la especificación deje de representar el comportamiento real
del sistema sin que exista una decisión registrada.

23. Excepciones

Puede ser necesario modificar el software antes de completar el ciclo
normal.

Ejemplos:

Vulnerabilidad de seguridad.
Error crítico en producción.
Corrupción de datos.
Interrupción de un servicio.
Problema de infraestructura.

En estos casos se prioriza la mitigación del riesgo.

Sin embargo, posteriormente debe realizarse la regularización documental:

Incidente
   ↓
Mitigación
   ↓
Registro
   ↓
Análisis
   ↓
Actualización de especificación
   ↓
Pruebas
   ↓
Verificación
24. Criterios de cierre

Una especificación puede considerarse cerrada cuando:

El comportamiento está definido.
Las decisiones relevantes están registradas.
La implementación correspondiente fue realizada.
Las pruebas fueron ejecutadas.
La implementación fue verificada.
La documentación relevante fue actualizada.
La trazabilidad está completa en el nivel requerido.

El cierre no implica que la especificación sea inmutable.

Puede volver a abrirse mediante un cambio formal.

25. Responsabilidad de Claude

Claude debe:

Respetar los estados de las especificaciones.
No implementar especificaciones que no estén suficientemente definidas.
Detectar inconsistencias entre código y especificación.
Identificar cambios que requieran volver a revisar una especificación.
Mantener la trazabilidad.
Informar cuando una transición de estado no sea válida.
Evitar modificaciones silenciosas.
Proponer cambios cuando el proceso existente resulte insuficiente.
26. Principio general

El ciclo de vida de una especificación debe permitir responder claramente:

¿Qué estamos construyendo?
¿Por qué lo estamos construyendo?
¿Quién aprobó el comportamiento?
¿Qué decisiones lo sustentan?
¿Está siendo implementado?
¿Ya fue implementado?
¿Fue verificado?
¿Sigue representando el comportamiento actual?
¿Qué cambios sufrió?
¿Qué pruebas demuestran su cumplimiento?

El objetivo del ciclo de vida no es crear burocracia.

Su objetivo es evitar que el conocimiento del sistema quede únicamente
implícito en el código o en la memoria de las personas que participaron en
su desarrollo.