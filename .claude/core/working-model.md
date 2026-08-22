# Modelo de Trabajo

Este documento define el modelo de colaboración entre el responsable del
proyecto y Claude durante el ciclo de vida del software.

El modelo se aplica a:

- Nuevos módulos.
- Nuevas funcionalidades.
- Cambios sobre funcionalidades existentes.
- Correcciones relevantes.
- Cambios arquitectónicos.
- Cambios de seguridad.
- Cambios de infraestructura.

El objetivo es mantener una separación clara entre:

- Análisis.
- Decisión.
- Especificación.
- Implementación.
- Verificación.

---

# 1. Principio general de trabajo

El desarrollo no debe comenzar directamente con la implementación.

El flujo general es:

```text
Necesidad
    ↓
Contexto
    ↓
Análisis
    ↓
Propuestas
    ↓
Validación humana
    ↓
Decisiones
    ↓
Especificación
    ↓
Plan de implementación
    ↓
Implementación
    ↓
Pruebas
    ↓
Verificación
    ↓
Cierre

No todas las tareas requieren el mismo nivel de formalidad.

Una modificación pequeña puede atravesar el proceso de forma simplificada,
mientras que una modificación de dominio, arquitectura o seguridad debe
seguir un proceso más riguroso.

2. Modos de trabajo

Claude debe identificar en qué modo se encuentra antes de comenzar una tarea.

Los principales modos son:

ANÁLISIS
ESPECIFICACIÓN
IMPLEMENTACIÓN
REVISIÓN
MANTENIMIENTO

Una conversación puede cambiar de modo explícitamente.

Claude no debe asumir que pasar de un modo a otro está autorizado cuando el
cambio implique decisiones relevantes.

3. Modo ANÁLISIS

El objetivo del modo análisis es comprender el problema antes de definir
una solución.

Durante esta fase Claude debe:

Leer el contexto relevante.
Identificar el problema.
Identificar actores.
Identificar requisitos conocidos.
Identificar restricciones.
Identificar dependencias.
Identificar conceptos del dominio.
Identificar módulos afectados.
Identificar estados y transiciones cuando corresponda.
Identificar invariantes.
Identificar riesgos.
Identificar decisiones pendientes.
Identificar contradicciones.
Proponer alternativas.
Recomendar una solución cuando corresponda.

Durante el análisis Claude no debe implementar cambios relevantes.

4. Lectura del contexto antes del análisis

Antes de analizar una funcionalidad, Claude debe consultar la documentación
y el código necesarios para comprender el contexto.

Como mínimo debe considerar:

CLAUDE.md
    ↓
Reglas especializadas relevantes
    ↓
Documentación general del proyecto
    ↓
Documentación del módulo afectado
    ↓
Decisiones relacionadas
    ↓
Especificaciones relacionadas
    ↓
Implementación existente
    ↓
Pruebas existentes

El orden exacto y los documentos obligatorios serán definidos por las reglas
de documentación y SDD.

Claude no debe leer archivos de forma indiscriminada.

Debe priorizar la información relevante para la tarea.

5. Resultado del análisis

Un análisis debe finalizar con una conclusión suficientemente clara para
permitir una decisión.

Cuando corresponda, Claude debe utilizar:

## Comprensión actual

## Hechos

## Suposiciones

## Problemas detectados

## Dependencias

## Riesgos

## Alternativas

## Recomendación

## Gaps

## Decisiones requeridas

La recomendación de Claude no constituye una decisión.

6. Revisión humana

Después del análisis, el responsable del proyecto revisa las propuestas.

Puede:

ACEPTAR
RECHAZAR
MODIFICAR
POSPONER
SOLICITAR MÁS ANÁLISIS

También puede introducir nuevas decisiones.

Claude debe incorporar únicamente las decisiones finales.

7. Aceptación parcial

Una propuesta puede aceptarse parcialmente.

Ejemplo:

PROPUESTA

A. Utilizar múltiples roles.
B. Los roles se almacenan en base de datos.
C. Los permisos se almacenan directamente en usuarios.

El responsable puede responder:

Aprobado:
A
B

Rechazado:
C

Modificación:
Los permisos pertenecen a roles.

Claude debe transformar esto en una decisión coherente:

Un usuario puede tener múltiples roles.
Los roles se almacenan en base de datos.
Los permisos pertenecen a roles.

No debe mantener las partes rechazadas como decisiones válidas.

8. Transición hacia ESPECIFICACIÓN

Una vez que las decisiones relevantes hayan sido tomadas, comienza la fase
de especificación.

La especificación debe transformar las decisiones en un comportamiento
describible y verificable.

Debe definir, cuando corresponda:

Objetivo.
Alcance.
Actores.
Requisitos.
Casos de uso.
API Contract, cuando corresponda.
Reglas de negocio.
Entidades.
Estados.
Transiciones.
Invariantes.
Errores.
Casos límite.
Criterios de aceptación.
Dependencias.
Restricciones técnicas relevantes.
Requisitos de seguridad.

No todos estos elementos serán necesarios para todas las funcionalidades.

La documentación debe ser proporcional a la complejidad del problema.

9. Revisión de la especificación

Una especificación no debe considerarse lista automáticamente al ser escrita
por Claude.

Debe revisarse para verificar:

Que represente las decisiones aprobadas.
Que no agregue requisitos no aprobados.
Que no contradiga requisitos existentes.
Que sea suficientemente precisa.
Que pueda verificarse.
Que tenga criterios de aceptación claros.
Que contemple casos relevantes.
Que identifique reglas de negocio.
Que identifique invariantes cuando existan.

Cuando existan decisiones importantes todavía abiertas, la especificación
debe reflejar el GAP en lugar de inventar una solución.

10. Aprobación de la especificación

La implementación de una funcionalidad relevante requiere que su
especificación esté suficientemente definida.

El responsable del proyecto puede:

APROBAR
RECHAZAR
SOLICITAR CAMBIOS

Una especificación aprobada se convierte en referencia para la
implementación.

11. Plan de implementación

Antes de realizar una implementación relevante, Claude debe determinar:

Qué componentes serán modificados.
Qué componentes serán creados.
Qué dependencias serán necesarias.
Qué contratos serán afectados.
Qué migraciones pueden ser necesarias.
Qué pruebas deben agregarse o modificarse.
Qué documentación deberá actualizarse.

El plan debe respetar la arquitectura y las decisiones aprobadas.

No debe utilizarse el plan para introducir decisiones que todavía no hayan
sido validadas.

12. Modo IMPLEMENTACIÓN

Durante la implementación Claude debe:

Seguir la especificación aprobada.
Respetar las decisiones registradas.
Respetar las reglas arquitectónicas.
Respetar las reglas de seguridad.
Mantener las responsabilidades separadas.
Crear o modificar pruebas correspondientes.
Evitar cambios no relacionados.
Mantener actualizada la documentación cuando corresponda.

La implementación no debe utilizarse para modificar silenciosamente el
alcance.

13. Decisiones descubiertas durante la implementación

Durante la implementación puede aparecer una decisión que no había sido
considerada.

Si la decisión es local y de bajo impacto, Claude puede resolverla según las
reglas de autoridad.

Si la decisión afecta:

Dominio.
Arquitectura.
Seguridad.
Persistencia.
Contratos.
Infraestructura.
Alcance.

Claude debe detener la parte afectada de la implementación y presentar el
problema.

Ejemplo:

DECISIÓN NO DEFINIDA

Durante la implementación se detectó que:

...

Esta decisión afecta:

...

Alternativas:

...

Decisión requerida:
...

No debe elegir silenciosamente una solución importante para continuar.

14. Modo REVISIÓN

Una vez implementada la funcionalidad, Claude debe realizar una revisión.

La revisión debe comparar:

Requisito
    ↓
Especificación
    ↓
Implementación
    ↓
Pruebas

Debe verificar:

Cobertura de requisitos.
Reglas de negocio.
Invariantes.
Estados.
Transiciones.
Contratos.
Seguridad.
Manejo de errores.
Pruebas.
Arquitectura.
Documentación.
15. Revisión crítica

La revisión no debe limitarse a comprobar si el código compila o si las
pruebas pasan.

Claude debe buscar también:

Complejidad innecesaria.
Acoplamiento.
Duplicación significativa.
Abstracciones prematuras.
Violaciones de arquitectura.
Problemas de seguridad.
Casos límite no contemplados.
Deuda técnica.
Contradicciones documentales.
Decisiones no registradas.
16. Modo MANTENIMIENTO

Cuando se modifique una funcionalidad existente, Claude debe tratar el
cambio como una evolución del sistema existente.

Antes de modificar debe identificar:

Qué comportamiento existe actualmente.
Qué documentación lo describe.
Qué decisiones lo originaron.
Qué dependencias existen.
Qué pruebas existen.
Qué otros módulos podrían verse afectados.

El cambio debe analizarse como una modificación del estado existente, no como
una funcionalidad completamente aislada.

17. Cambios sobre documentación existente

Cuando una modificación afecte documentación existente, Claude debe:

Identificar los documentos afectados.
Leer su contenido actual.
Determinar qué información sigue siendo válida.
Determinar qué información quedó obsoleta.
Identificar decisiones afectadas.
Identificar trazabilidad afectada.
Proponer los cambios necesarios.
Actualizar la documentación correspondiente después de la decisión.

No debe reescribir documentación completa si solamente es necesario modificar
una parte.

18. No borrar historia de decisiones

Cuando una decisión cambie, no debe eliminarse el conocimiento histórico de
forma que resulte imposible comprender qué se había decidido anteriormente.

Las decisiones reemplazadas deben poder identificarse como tales cuando el
mecanismo documental correspondiente lo permita.

La documentación actual debe representar la decisión vigente.

El historial debe conservarse mediante el mecanismo de versionado o registro
de decisiones definido por el proyecto.

19. Proporcionalidad

No todas las tareas requieren el mismo proceso.

Cambio pequeño

Puede seguir:

Contexto
 ↓
Análisis
 ↓
Implementación
 ↓
Prueba
 ↓
Revisión
Nueva funcionalidad

Debe seguir:

Análisis
 ↓
Decisiones
 ↓
Especificación
 ↓
Implementación
 ↓
Pruebas
 ↓
Verificación
Cambio de dominio, arquitectura o seguridad

Debe seguir un proceso más riguroso:

Contexto
 ↓
Análisis
 ↓
Alternativas
 ↓
Evaluación de impacto
 ↓
Decisión humana
 ↓
Especificación
 ↓
Revisión
 ↓
Plan
 ↓
Implementación
 ↓
Pruebas
 ↓
Verificación
 ↓
Actualización documental
20. Criterios de finalización

Una tarea no debe considerarse terminada simplemente porque el código fue
escrito.

Según corresponda, debe verificarse:

[ ] Requisitos definidos
[ ] Decisiones registradas
[ ] Especificación actualizada
[ ] Implementación realizada
[ ] Pruebas creadas o actualizadas
[ ] Pruebas ejecutadas
[ ] Seguridad revisada
[ ] Arquitectura revisada
[ ] Documentación actualizada
[ ] Trazabilidad actualizada
[ ] Revisión final realizada

No todos los puntos son obligatorios para todas las tareas.

Los elementos aplicables deben determinarse según el tipo y riesgo del
cambio.

21. Comunicación entre etapas

Claude debe indicar claramente cuándo está cambiando de etapa.

Por ejemplo:

MODO ACTUAL: ANÁLISIS

Resultado:
...

DECISIONES REQUERIDAS:
...

Después de la aprobación:

MODO ACTUAL: ESPECIFICACIÓN

Decisiones incorporadas:
...

Durante la implementación:

MODO ACTUAL: IMPLEMENTACIÓN

Especificación:
...
Componentes afectados:
...

Durante la revisión:

MODO ACTUAL: REVISIÓN

Resultado:
...
Problemas encontrados:
...

Esto permite distinguir una recomendación de una decisión y una decisión de
una implementación.

22. Comunicación de bloqueos

Claude debe detenerse cuando una decisión necesaria no esté definida y tenga
impacto relevante.

Debe evitar continuar mediante una suposición silenciosa.

El bloqueo debe expresarse de forma concreta:

BLOQUEO

Para continuar es necesario decidir:

...

Motivo:
...

Opciones:
...

Recomendación:
...
23. Separación entre análisis e implementación

Claude no debe mezclar automáticamente el análisis con la implementación.

Cuando el responsable solicite únicamente análisis, Claude debe limitarse al
análisis.

Cuando el responsable solicite implementación de una especificación
aprobada, Claude puede implementar.

Cuando el responsable solicite una revisión, Claude debe evaluar la solución
existente sin asumir que debe modificarla inmediatamente.

24. Flujo general de un módulo

Un módulo nuevo debe desarrollarse siguiendo, cuando corresponda, este
modelo:

1. Identificar el problema
          ↓
2. Analizar contexto
          ↓
3. Identificar dependencias
          ↓
4. Identificar requisitos
          ↓
5. Modelar dominio
          ↓
6. Analizar casos de uso
          ↓
7. Identificar estados e invariantes
          ↓
8. Analizar arquitectura
          ↓
9. Analizar seguridad
          ↓
10. Presentar propuestas
          ↓
11. Validación humana
          ↓
12. Registrar decisiones
          ↓
13. Crear especificaciones
          ↓
14. Aprobar especificaciones
          ↓
15. Crear plan de implementación
          ↓
16. Implementar
          ↓
17. Crear/actualizar pruebas
          ↓
18. Verificar
          ↓
19. Actualizar documentación
          ↓
20. Cerrar módulo o iteración

Este flujo es una guía general.

Los detalles específicos de cada etapa se encuentran en las reglas de
SDD, DDD, arquitectura, documentación, desarrollo y seguridad.

25. Flujo general de un caso de uso

Cuando una funcionalidad pueda expresarse como un caso de uso, el proceso
general será:

Problema
   ↓
Actor
   ↓
Objetivo
   ↓
Precondiciones
   ↓
Flujo principal
   ↓
Flujos alternativos
   ↓
Errores
   ↓
Reglas de negocio
   ↓
Invariantes
   ↓
Criterios de aceptación
   ↓
Implementación
   ↓
Pruebas
   ↓
Verificación

No todos los casos de uso requerirán exactamente los mismos elementos.

26. Trazabilidad durante el ciclo de vida

La trazabilidad debe mantenerse durante todo el proceso.

Una funcionalidad debería poder relacionarse con:

Requisito
   ↓
Caso de uso
   ↓
Especificación
   ↓
Decisión
   ↓
Implementación
   ↓
Prueba

Cuando una modificación rompe alguna relación existente, Claude debe
identificarla y actualizar la trazabilidad correspondiente.

27. El proceso también puede cambiar

Este modelo de trabajo no es inmutable.

Si durante el desarrollo se descubre que una etapa:

No aporta valor.
Es insuficiente.
Genera burocracia.
Produce documentación redundante.
No permite controlar correctamente los cambios.

Claude debe proponer una modificación del proceso.

La modificación debe ser evaluada y aprobada antes de convertirse en una
nueva regla del proyecto.

28. Principio general

El modelo de trabajo busca mantener una separación clara:

Claude piensa y propone.
        ↓
El responsable decide.
        ↓
Claude especifica.
        ↓
El responsable valida cuando corresponde.
        ↓
Claude implementa.
        ↓
Claude verifica.
        ↓
El sistema y su documentación evolucionan juntos.

La finalidad no es agregar pasos burocráticos.

La finalidad es evitar que una decisión importante se transforme
accidentalmente en código antes de haber sido comprendida, discutida y
validada.