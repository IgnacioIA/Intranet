# Gestión de Cambios

Este documento define cómo deben analizarse, aprobarse, documentarse,
implementar y verificar los cambios que afecten al sistema.

El objetivo es garantizar que una modificación del software no se limite a
cambiar código, sino que permita comprender:

- Qué cambió.
- Por qué cambió.
- Quién tomó la decisión.
- Qué documentación fue afectada.
- Qué componentes fueron afectados.
- Qué pruebas deben modificarse.
- Qué riesgos introduce.
- Cómo verificar que el cambio fue implementado correctamente.

---

# 1. Principio general

Un cambio relevante debe considerarse una modificación del conocimiento y
del comportamiento del sistema, no solamente una modificación del código.

El flujo general es:

```text
Solicitud de cambio
        ↓
Comprensión
        ↓
Análisis de impacto
        ↓
Alternativas
        ↓
Decisión
        ↓
Actualización de especificación
        ↓
Plan de implementación
        ↓
Implementación
        ↓
Pruebas
        ↓
Verificación
        ↓
Actualización de trazabilidad

La profundidad del proceso debe ser proporcional al impacto del cambio.

2. ¿Qué se considera un cambio?

Un cambio puede afectar:

Requisitos.
Reglas de negocio.
Casos de uso.
Entidades.
Estados.
Transiciones.
Invariantes.
APIs.
Persistencia.
Arquitectura.
Seguridad.
Infraestructura.
Integraciones.
Permisos.
Roles.
Comportamiento existente.
Criterios de aceptación.

No todo cambio requiere el mismo nivel de formalidad.

3. Cambios menores

Un cambio puede considerarse menor cuando:

Tiene bajo impacto.
Es fácilmente reversible.
No modifica el comportamiento del dominio.
No modifica contratos.
No modifica seguridad.
No afecta otros módulos.
No altera decisiones arquitectónicas.
No requiere migraciones significativas.

Ejemplos:

Corrección de un typo.
Mejora de nombres internos.
Refactorización sin cambio de comportamiento.
Corrección de documentación sin cambio conceptual.

Estos cambios pueden utilizar un flujo simplificado.

Cambio
  ↓
Análisis
  ↓
Implementación
  ↓
Prueba
  ↓
Verificación
4. Cambios significativos

Un cambio debe considerarse significativo cuando modifica el comportamiento
de una funcionalidad, módulo o contrato.

Ejemplos:

Agregar una capacidad.
Modificar un caso de uso.
Cambiar una regla de negocio.
Agregar un estado.
Eliminar una transición.
Modificar permisos.
Modificar una API.
Modificar una entidad.
Modificar una relación entre módulos.

El flujo debe ser:

Cambio
  ↓
Análisis de impacto
  ↓
Decisión
  ↓
Especificación
  ↓
Implementación
  ↓
Pruebas
  ↓
Verificación
5. Cambios críticos

Un cambio debe considerarse crítico cuando afecta significativamente:

Seguridad.
Autenticación.
Autorización.
Datos sensibles.
Persistencia.
Arquitectura.
Infraestructura.
Disponibilidad.
Integridad de datos.
Contratos externos.
Migraciones importantes.

Estos cambios requieren un análisis más profundo y validación humana
explícita.

El proceso debe incluir:

Cambio
  ↓
Análisis
  ↓
Impacto
  ↓
Riesgos
  ↓
Alternativas
  ↓
Decisión humana
  ↓
Documentación
  ↓
Implementación
  ↓
Pruebas
  ↓
Verificación
6. El cambio debe tener un motivo

Todo cambio significativo debe poder responder:

¿Por qué se realiza este cambio?

Los motivos pueden ser:

Nuevo requisito.
Cambio de negocio.
Error de diseño.
Error de especificación.
Error de implementación.
Requisito de seguridad.
Requisito de infraestructura.
Mejora de mantenibilidad.
Cambio de integración.
Descubrimiento durante el desarrollo.
Necesidad operativa.

No debe realizarse un cambio relevante simplemente porque una solución
"parece mejor" sin analizar su impacto.

7. Solicitud de cambio

Cuando se identifica un cambio relevante debe registrarse su intención.

La solicitud debe incluir, cuando corresponda:

Identificador
Título
Motivo
Descripción
Solicitante
Prioridad
Alcance
Módulos potencialmente afectados

Ejemplo:

CR-COM-003

Título:
Agregar fecha de expiración a comunicados.

Motivo:
Los comunicados deben dejar de mostrarse automáticamente
después de una fecha determinada.

La convención concreta de identificadores será definida por las reglas de
documentación.

8. No implementar directamente una solicitud de cambio

Una solicitud de cambio no equivale automáticamente a una decisión.

El flujo correcto es:

Solicitud
   ↓
Análisis
   ↓
Propuesta
   ↓
Decisión

Claude no debe interpretar una solicitud como autorización automática para
modificar el dominio, arquitectura o comportamiento del sistema cuando el
impacto sea relevante.

9. Análisis de impacto

Antes de aprobar un cambio significativo debe analizarse su impacto.

Como mínimo debe considerarse:

Requisitos afectados.
Especificaciones afectadas.
Casos de uso afectados.
Entidades afectadas.
Estados afectados.
Transiciones afectadas.
Invariantes afectadas.
Módulos afectados.
APIs afectadas.
Base de datos afectada.
Pruebas afectadas.
Seguridad afectada.
Infraestructura afectada.
Documentación afectada.
Trazabilidad afectada.
10. Impacto directo e indirecto

Claude debe diferenciar:

Impacto directo

Componentes que necesariamente deben modificarse.

Ejemplo:

Cambio de estado de Comunicados
        ↓
Comunicado
ComunicadoService
Pruebas de Comunicados
Impacto indirecto

Componentes que podrían verse afectados por el cambio.

Ejemplo:

Cambio de estado de Comunicados
        ↓
Notificaciones
Auditoría
Buscador
Frontend

Los impactos indirectos deben investigarse antes de implementar cuando
exista riesgo significativo.

11. Análisis de dependencias

Debe identificarse qué componentes dependen del comportamiento que está
siendo modificado.

Ejemplo:

SPEC-COM-003
Publicar comunicado
        ↓
        ├── Notificaciones
        ├── Auditoría
        ├── Buscador
        └── Permisos

Cambiar la especificación puede afectar estos componentes aunque no sean
parte directa de la funcionalidad modificada.

12. Análisis de seguridad

Todo cambio debe analizar si afecta la seguridad.

Especialmente:

Autenticación.
Autorización.
Roles.
Permisos.
Información sensible.
Auditoría.
Logs.
Exposición de APIs.
Validaciones.
Acceso a recursos.
Integridad de datos.

Si el cambio tiene impacto de seguridad, debe elevarse su nivel de análisis.

13. Análisis de datos

Cuando un cambio afecte persistencia, debe analizarse:

Modelo de datos.
Entidades.
Relaciones.
Restricciones.
Índices.
Datos existentes.
Migraciones.
Compatibilidad.
Rollback.
Integridad.

No debe asumirse que un cambio de código puede realizarse sin considerar
los datos existentes.

14. Análisis de API y contratos

Si el cambio modifica un contrato, deben identificarse:

Endpoints.
Request.
Response.
Errores.
Códigos HTTP.
Autorización.
Compatibilidad.
Consumidores.

Un cambio de contrato debe considerarse especialmente sensible si existen
consumidores externos o múltiples módulos dependientes.

15. Alternativas

Cuando el cambio implique una decisión de diseño, Claude debe presentar
alternativas cuando sea útil.

Cada alternativa debería considerar:

Descripción.
Ventajas.
Desventajas.
Complejidad.
Riesgos.
Impacto.
Reversibilidad.
Coste de implementación.

Claude debe recomendar una alternativa cuando tenga fundamentos suficientes.

La recomendación no equivale a aprobación.

16. Decisión del cambio

Después del análisis, el responsable del proyecto puede:

ACEPTAR
RECHAZAR
MODIFICAR
POSPONER
SOLICITAR MÁS ANÁLISIS

La decisión debe quedar clara.

Ejemplo:

DECISIÓN

Se aprueba agregar una fecha de expiración a los comunicados.

La expiración será opcional.

Un comunicado sin fecha de expiración permanecerá visible
hasta ser archivado.
17. Cambios rechazados

Cuando un cambio sea rechazado:

No debe implementarse.
Debe conservarse el motivo cuando sea relevante.
No debe eliminarse la documentación relacionada.
Puede volver a analizarse posteriormente.

Ejemplo:

CR-COM-004

Estado:
RECHAZADO

Motivo:
Se decidió no incorporar publicación automática recurrente
debido a la complejidad innecesaria para la primera versión.
18. Cambios pospuestos

Un cambio puede ser válido pero no prioritario.

En ese caso debe registrarse como pendiente.

Debe indicarse:

Motivo.
Prioridad.
Dependencias.
Condiciones necesarias para retomarlo.

Un cambio pospuesto no debe confundirse con un cambio rechazado.

19. Actualización de la especificación

Una vez aprobado un cambio que modifica comportamiento, la especificación
correspondiente debe actualizarse antes o como parte del proceso de
implementación.

El objetivo es evitar:

Código nuevo
    ↓
Especificación vieja

El estado de la especificación debe evolucionar según:

.claude/sdd/lifecycle.md
20. Cambio de una especificación aprobada

Una especificación APPROVED no debe modificarse directamente sin analizar
el cambio.

El proceso es:

Especificación APPROVED
        ↓
Solicitud de cambio
        ↓
Análisis de impacto
        ↓
Decisión
        ↓
Nueva revisión de especificación
        ↓
IN_REVIEW
        ↓
APPROVED

Si el cambio es rechazado, la especificación original permanece vigente.

21. Cambios sobre especificaciones verificadas

Una especificación VERIFIED representa comportamiento que ya fue
implementado y comprobado.

Si se modifica, debe considerarse que:

Comportamiento anterior
        ↓
Cambio aprobado
        ↓
Nuevo comportamiento esperado

La implementación anterior no debe considerarse automáticamente válida para
la nueva especificación.

Deben identificarse las pruebas que deben modificarse.

22. Preservación de historial

Cuando una especificación evoluciona, no debe eliminarse información
histórica relevante.

Debe poder conocerse:

Qué comportamiento existía.
Qué cambió.
Por qué cambió.
Qué decisión originó el cambio.

El historial técnico debe apoyarse en:

Git.
ADR.
Historial de cambios.
Versionado de documentación.
Sistema de trazabilidad.

La documentación vigente debe seguir siendo clara y no convertirse en un
registro histórico ilegible.

23. Cambio que invalida una decisión anterior

Puede ocurrir que un cambio demuestre que una decisión anterior ya no es
adecuada.

Ejemplo:

ADR-005
Un usuario posee un único rol.

Posteriormente:

Cambio aprobado:
Un usuario puede poseer múltiples roles.

Claude debe:

Identificar la decisión anterior.
Identificar el conflicto.
Proponer la modificación.
Registrar la nueva decisión.
Identificar los artefactos afectados.
Actualizar la documentación correspondiente.
Verificar la implementación.

No debe simplemente modificar el código.

24. Cambio que revela un error en la especificación

Puede descubrirse que una especificación aprobada era incorrecta.

Por ejemplo:

La especificación permite eliminar usuarios.

Durante el análisis se descubre que esto contradice una regla de negocio.

Debe seguirse:

Error detectado
      ↓
Análisis
      ↓
Decisión
      ↓
Corrección de especificación
      ↓
Análisis de impacto
      ↓
Implementación
      ↓
Pruebas

Debe registrarse que la modificación corrigió un error y no simplemente que
"se cambió el comportamiento".

25. Cambio que revela un problema arquitectónico

Durante la implementación puede aparecer un cambio que no pueda resolverse
correctamente sin modificar la arquitectura.

Ejemplo:

Nueva funcionalidad
        ↓
Requiere comunicación entre módulos
        ↓
La arquitectura actual no permite esa dependencia

Claude no debe introducir una dependencia arbitraria.

Debe detenerse y presentar:

Problema arquitectónico
Alternativas
Impacto
Recomendación
Decisión requerida

Si se modifica la arquitectura, debe aplicarse el proceso correspondiente
de arquitectura y ADR.

26. Cambios de seguridad

Los cambios relacionados con seguridad deben recibir especial atención.

Ejemplos:

Nuevo rol.
Nuevo permiso.
Cambio en autenticación.
Acceso a nueva información.
Exposición de un endpoint.
Cambio en auditoría.
Cambio en gestión de credenciales.

Debe analizarse:

Amenaza
   ↓
Superficie afectada
   ↓
Controles existentes
   ↓
Nuevo riesgo
   ↓
Mitigación

Cuando corresponda, el cambio debe generar o actualizar documentación de
seguridad.

27. Cambios urgentes

Puede existir una situación en la que no sea posible completar el proceso
normal antes de intervenir.

Ejemplos:

Vulnerabilidad crítica.
Incidente de producción.
Corrupción de datos.
Caída de un servicio esencial.
Pérdida de integridad.

En estos casos:

Detección
   ↓
Mitigación
   ↓
Restauración
   ↓
Registro
   ↓
Análisis posterior
   ↓
Regularización documental

La urgencia puede justificar modificar temporalmente el orden del proceso,
pero no elimina la necesidad de documentar posteriormente el cambio.

28. Rollback y reversibilidad

Antes de implementar un cambio significativo debe evaluarse su
reversibilidad.

Debe considerarse:

¿Puede revertirse el código?
¿Puede revertirse la base de datos?
¿Puede revertirse la configuración?
¿Puede revertirse una migración?
¿Puede restaurarse la información?
¿Qué ocurre con datos creados durante el cambio?

Los cambios difíciles de revertir deben recibir mayor nivel de análisis.

29. Compatibilidad

Cuando un cambio afecta un contrato o comportamiento existente debe
evaluarse la compatibilidad.

Debe determinarse si el cambio es:

COMPATIBLE

o:

BREAKING CHANGE

Ejemplos de posibles cambios incompatibles:

Eliminar un endpoint.
Cambiar una respuesta.
Cambiar una regla de autorización.
Eliminar un campo utilizado por otro componente.
Modificar una transición que otro módulo utiliza.

Los breaking changes requieren un análisis de impacto explícito.

30. Pruebas afectadas

Todo cambio relevante debe identificar qué pruebas deben:

Crear.
Modificar.
Eliminar.
Mantenerse.

La eliminación de una prueba no debe utilizarse simplemente para hacer que
el conjunto de pruebas vuelva a pasar.

Debe existir una razón relacionada con el cambio de comportamiento.

31. Documentación afectada

Un cambio debe identificar los documentos afectados.

Pueden incluir:

Requisitos
Especificaciones
Casos de uso
Entidades
Estados
Transiciones
Invariantes
Arquitectura
ADR
Seguridad
Infraestructura
Trazabilidad

Claude debe evitar actualizar documentación que no esté relacionada.

32. Trazabilidad del cambio

Un cambio relevante debe poder rastrearse:

Solicitud de cambio
        ↓
Decisión
        ↓
Especificación
        ↓
Implementación
        ↓
Prueba
        ↓
Verificación

El mecanismo concreto de trazabilidad se define en:

.claude/sdd/traceability.md
33. Implementación de un cambio

Una vez aprobado:

Actualizar la especificación correspondiente.
Actualizar decisiones relacionadas.
Crear o modificar el plan de implementación.
Modificar el código.
Modificar las pruebas.
Ejecutar las pruebas.
Realizar revisión.
Actualizar la trazabilidad.
Verificar la documentación.

No debe implementarse un cambio significativo únicamente en el código.

34. Cambios descubiertos durante la implementación

Puede aparecer un cambio adicional durante la implementación.

Ejemplo:

Cambio original
      ↓
Implementación
      ↓
Se descubre otra dependencia
      ↓
Nuevo cambio potencial

Claude debe diferenciar:

Cambio necesario para implementar lo aprobado

de:

Nuevo cambio de alcance

Si es un nuevo cambio de alcance, debe volver al proceso de análisis.

No debe incorporarse silenciosamente.

35. Cambios de alcance

Si durante una tarea aparece una funcionalidad que no estaba incluida en el
alcance original, debe identificarse.

Ejemplo:

Alcance aprobado:
Crear y publicar comunicados.

Durante implementación:
Se propone agregar programación automática.

La programación automática es un cambio de alcance.

Debe tratarse como una nueva decisión.

36. Priorización

Los cambios pueden clasificarse según prioridad.

La clasificación concreta puede evolucionar con el proyecto.

Como mínimo debe distinguirse entre:

CRÍTICO
ALTO
MEDIO
BAJO

La prioridad debe considerar:

Impacto.
Riesgo.
Dependencias.
Urgencia.
Coste.
Valor para el negocio.
37. Coste del cambio

Antes de aprobar cambios significativos, Claude debe intentar estimar su
impacto.

La estimación puede considerar:

Análisis.
Documentación.
Desarrollo.
Migraciones.
Pruebas.
Infraestructura.
Despliegue.
Revisión.

Las estimaciones son aproximaciones y no deben presentarse como certezas.

38. No optimizar estimaciones ocultando trabajo

Claude no debe reducir artificialmente una estimación ignorando:

Pruebas.
Documentación.
Migraciones.
Seguridad.
Revisión.
Integración.
Despliegue.

El objetivo es obtener una estimación útil para la planificación real.

39. Cierre del cambio

Un cambio puede considerarse cerrado cuando:

[ ] Cambio comprendido
[ ] Impacto analizado
[ ] Decisión tomada
[ ] Especificación actualizada
[ ] Decisiones relacionadas actualizadas
[ ] Implementación completada
[ ] Pruebas actualizadas
[ ] Pruebas ejecutadas
[ ] Seguridad revisada
[ ] Documentación actualizada
[ ] Trazabilidad actualizada
[ ] Verificación completada

Los elementos aplicables dependen del tipo de cambio.

40. Responsabilidad de Claude

Claude debe:

Detectar cambios.
Diferenciar cambios menores de cambios relevantes.
Analizar impacto.
Identificar dependencias.
Detectar cambios de alcance.
Identificar conflictos con decisiones existentes.
Proponer alternativas.
Recomendar soluciones.
Actualizar la documentación correspondiente.
Mantener trazabilidad.
Verificar que la implementación corresponda al cambio aprobado.

Claude no debe:

Introducir cambios de alcance silenciosamente.
Modificar especificaciones para justificar código.
Eliminar historial relevante.
Ignorar dependencias.
Ocultar impactos.
Implementar cambios críticos sin aprobación.
Eliminar pruebas únicamente porque fallen después de un cambio.
41. Responsabilidad del responsable del proyecto

El responsable del proyecto debe:

Definir prioridades.
Evaluar propuestas.
Aprobar o rechazar cambios relevantes.
Resolver conflictos de negocio.
Aprobar cambios arquitectónicos o de seguridad cuando corresponda.
Determinar cuándo un cambio forma parte del alcance.
Validar decisiones importantes.

No es necesario que apruebe cada cambio técnico menor.

La autoridad debe ser proporcional al impacto.

42. Principio general

Un sistema mantenible no es aquel que nunca cambia.

Es aquel en el que los cambios pueden realizarse sin perder el conocimiento
de por qué el sistema funciona como funciona.

Por lo tanto:

Cambio
  ↓
Comprender
  ↓
Analizar
  ↓
Decidir
  ↓
Documentar
  ↓
Implementar
  ↓
Verificar

La documentación debe evolucionar junto con el software.

El código y la especificación deben converger hacia el mismo comportamiento
esperado.

El historial de decisiones debe permitir comprender cómo y por qué el
sistema llegó a su estado actual.