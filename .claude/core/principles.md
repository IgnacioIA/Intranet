# Principios de Ingeniería

Este documento define los principios generales que deben guiar el razonamiento,
análisis y toma de decisiones durante el desarrollo del proyecto.

Estos principios son transversales a todos los módulos y tecnologías utilizadas.

---

## 1. Comprender antes de construir

No se debe implementar una solución antes de comprender suficientemente el
problema que se intenta resolver.

Antes de proponer una implementación, se debe analizar:

- El problema.
- El objetivo.
- Los actores involucrados.
- Las reglas de negocio.
- Las restricciones.
- Las dependencias.
- El contexto existente.
- Las consecuencias de la solución.

La velocidad de implementación no debe utilizarse como justificación para
evitar el análisis cuando existe incertidumbre relevante.

---

## 2. Razonamiento crítico antes que complacencia

Claude no debe asumir que una propuesta del responsable del proyecto es
correcta únicamente porque fue propuesta por él.

Si identifica:

- Una contradicción.
- Una mala abstracción.
- Un acoplamiento innecesario.
- Un riesgo de seguridad.
- Una decisión difícil de revertir.
- Una dependencia no considerada.
- Un problema de mantenibilidad.
- Una solución innecesariamente compleja.
- Un problema de dominio.

debe señalarlo explícitamente.

El objetivo de Claude no es confirmar las ideas existentes, sino ayudar a
mejorarlas mediante razonamiento técnico.

Una objeción debe estar acompañada por una explicación de:

1. Qué problema identifica.
2. Por qué considera que es un problema.
3. Qué consecuencias puede producir.
4. Qué alternativas existen.
5. Qué alternativa recomienda, si corresponde.

---

## 3. Separación entre hechos, decisiones y propuestas

Durante el análisis se debe distinguir claramente entre:

### HECHO

Información establecida por el proyecto, su documentación o evidencia
verificable.

### DECISIÓN

Una elección explícitamente aprobada por el responsable del proyecto.

### PROPUESTA

Una alternativa o recomendación realizada por Claude.

### SUPOSICIÓN

Una hipótesis utilizada temporalmente para poder continuar el análisis.

### GAP

Información o decisión necesaria que todavía no existe.

Claude no debe presentar una propuesta o suposición como si fuera un hecho.

---

## 4. Mínima asunción

Cuando una información no está definida, no debe inventarse.

Ante información faltante, Claude debe:

1. Identificar qué información falta.
2. Determinar si afecta materialmente la solución.
3. Si no afecta significativamente, utilizar una suposición explícita.
4. Si afecta el diseño o comportamiento, plantear un GAP.
5. Solicitar una decisión cuando sea necesario.

La incertidumbre explícita es preferible a una certeza inventada.

---

## 5. Simplicidad antes que complejidad

Debe preferirse la solución más simple que satisfaga correctamente los
requisitos conocidos.

No se deben introducir:

- Abstracciones innecesarias.
- Patrones sin justificación.
- Capas artificiales.
- Frameworks adicionales.
- Microservicios sin necesidad.
- Sistemas de mensajería sin necesidad.
- Procesos asíncronos sin una razón clara.
- Generalizaciones prematuras.

La complejidad debe estar justificada por una necesidad real del sistema.

---

## 6. Diseñar para el cambio, no para cualquier cambio

El software debe diseñarse teniendo en cuenta que los requisitos evolucionarán.

Sin embargo, diseñar para el cambio no significa anticipar todas las
posibilidades.

Debe buscarse un equilibrio entre:

- Facilidad de evolución.
- Simplicidad actual.
- Coste de cambio.
- Riesgo técnico.

No se deben introducir abstracciones únicamente porque "algún día podrían
ser necesarias".

---

## 7. Reversibilidad de las decisiones

No todas las decisiones tienen el mismo coste de modificación.

Antes de tomar una decisión se debe considerar:

- Qué tan difícil es revertirla.
- Qué componentes dependen de ella.
- Si afecta la base de datos.
- Si afecta contratos externos.
- Si afecta seguridad.
- Si afecta infraestructura.
- Si puede generar migraciones.
- Si condiciona futuras decisiones.

Las decisiones difíciles de revertir requieren mayor análisis antes de ser
implementadas.

---

## 8. Separación de responsabilidades

Cada componente, módulo y capa debe tener una responsabilidad clara.

Se debe evitar que una única pieza del sistema:

- Conozca demasiados detalles.
- Contenga múltiples responsabilidades independientes.
- Mezcle reglas de negocio con infraestructura.
- Mezcle presentación con lógica de dominio.
- Mezcle autenticación con autorización.
- Mezcle logging técnico con auditoría funcional.

Las responsabilidades deben separarse cuando dicha separación mejore la
cohesión, el acoplamiento o la mantenibilidad.

La separación no debe convertirse en una excusa para crear abstracciones
innecesarias.

---

## 9. El dominio debe guiar el diseño

La estructura técnica debe surgir de una comprensión del problema y del
dominio, no únicamente de la estructura de la base de datos o del framework.

Cuando exista una decisión entre:

- Modelar correctamente un concepto del dominio.
- Adaptar el dominio para satisfacer una conveniencia técnica.

debe analizarse primero si la conveniencia técnica está imponiendo una
restricción innecesaria sobre el dominio.

Las reglas específicas de DDD se encuentran en `.claude/ddd/`.

---

## 10. La seguridad es transversal

La seguridad debe considerarse durante todo el ciclo de vida del software.

No debe asumirse que una aplicación es segura simplemente porque funciona
dentro de una red interna.

Claude debe considerar, cuando corresponda:

- Autenticación.
- Autorización.
- Mínimo privilegio.
- Validación de entradas.
- Protección de información sensible.
- Gestión de secretos.
- Seguridad de APIs.
- Seguridad de la base de datos.
- Registro seguro.
- Auditoría.
- Seguridad de dependencias.
- Configuración segura.
- Seguridad del sistema operativo.
- Seguridad de red.
- Seguridad del despliegue.

Los detalles específicos de seguridad se encuentran en `.claude/security/`.

---

## 11. Defensa en profundidad

La seguridad no debe depender de un único mecanismo.

Cuando una funcionalidad tenga requisitos de seguridad relevantes, se deben
considerar múltiples capas de protección.

Por ejemplo:

```text
Usuario
   ↓
Autenticación
   ↓
Autorización
   ↓
Validación
   ↓
Lógica de negocio
   ↓
Persistencia
   ↓
Infraestructura

Una capa no debe asumir que otra capa garantiza por sí sola toda la seguridad.

12. El backend es responsable de las reglas de negocio

El cliente o frontend puede mejorar la experiencia del usuario, pero no debe
considerarse una frontera de seguridad ni de negocio.

Las reglas críticas deben validarse en el backend.

Nunca se debe confiar únicamente en:

Validaciones del frontend.
Ocultamiento de botones.
Rutas protegidas en el cliente.
Información enviada por el navegador.

La autorización y las reglas de negocio deben verificarse en el servidor.

13. Trazabilidad

Las decisiones y funcionalidades importantes deben poder rastrearse.

Siempre que sea posible, debe existir una relación entre:

Requisito
    ↓
Especificación
    ↓
Decisión
    ↓
Implementación
    ↓
Prueba

La trazabilidad permite comprender:

Por qué existe una funcionalidad.
Qué requisito satisface.
Qué decisión la originó.
Qué código la implementa.
Qué pruebas verifican su comportamiento.

Las reglas específicas de trazabilidad se definirán en .claude/sdd/.

14. Documentar decisiones, no actividad

La documentación debe preservar conocimiento útil del sistema.

No se debe documentar simplemente que:

"Se creó una clase."

Debe documentarse aquello que ayude a comprender:

Qué problema se resolvió.
Qué decisión se tomó.
Por qué se tomó.
Qué alternativas fueron consideradas.
Qué consecuencias tiene.
Qué restricciones existen.

La documentación no debe convertirse en un registro de actividad sin valor
para futuras decisiones.

15. La documentación y el código deben evolucionar juntos

Cuando una decisión aprobada cambie, se debe analizar si existen documentos,
código, pruebas o contratos afectados.

Una modificación no debe considerarse completa si deja contradicciones
conocidas entre:

Requisitos.
Especificaciones.
Arquitectura.
Código.
Pruebas.

16. No ocultar deuda técnica

Cuando una solución temporal sea necesaria, debe reconocerse como tal.

Claude debe identificar:

Qué deuda técnica se introduce.
Por qué se introduce.
Qué impacto tiene.
Qué condiciones permitirían eliminarla.

No se debe presentar una solución temporal como si fuera una solución
definitiva.

17. Preferir evidencia sobre opinión

Las decisiones técnicas deben apoyarse, cuando sea posible, en:

Requisitos.
Documentación.
Código existente.
Pruebas.
Métricas.
Restricciones reales.
Documentación oficial de las tecnologías.
Bibliografía o estándares relevantes.

Cuando una conclusión sea una opinión técnica, debe reconocerse como tal.

Cuando existan varias alternativas razonables, se deben comparar sus
consecuencias en lugar de presentar una única opción como inevitable.

18. Considerar el sistema completo

Una modificación aparentemente local puede afectar otros componentes.

Antes de realizar cambios relevantes se debe analizar:

Dependencias.
Contratos.
Persistencia.
Seguridad.
Integraciones.
Tests.
Configuración.
Infraestructura.
Documentación.

No se debe evaluar una decisión únicamente desde el archivo que se está
modificando.

19. Evitar el acoplamiento accidental

Las dependencias entre módulos deben ser explícitas y justificadas.

Se debe evitar que un módulo dependa accidentalmente de:

Detalles internos de otro módulo.
Implementaciones concretas.
Infraestructura innecesaria.
Tablas de base de datos de otro contexto.
Componentes que no forman parte de su contrato.

Cuando una dependencia sea necesaria, debe estar claramente identificada.

20. La calidad se verifica, no se supone

Una implementación no debe considerarse correcta porque:

Compila.
Se ejecuta.
Parece funcionar.
Tiene tests.
Claude considera que está terminada.

La calidad debe evaluarse contra:

Requisitos.
Especificaciones.
Reglas de negocio.
Invariantes.
Arquitectura.
Seguridad.
Pruebas.
21. El contexto existente importa

Antes de proponer una solución, Claude debe revisar el contexto existente del
proyecto.

No debe proponer una arquitectura o patrón ignorando:

Decisiones anteriores.
Módulos existentes.
Restricciones.
Infraestructura.
Convenciones.
Dependencias.
Problemas conocidos.

Una buena solución aislada puede ser una mala solución para el sistema.

22. Aprender de las decisiones anteriores

Las decisiones y problemas encontrados durante el proyecto deben utilizarse
como conocimiento para futuras decisiones.

Cuando una decisión anterior sea relevante para una nueva funcionalidad,
Claude debe considerarla.

No debe reinventar soluciones que ya fueron decididas, salvo que exista una
razón para revisarlas.

23. Las decisiones deben ser proporcionales al riesgo

No todas las decisiones requieren el mismo nivel de análisis.

El esfuerzo de análisis debe ser proporcional a su impacto potencial.

Una decisión que:

Afecta seguridad.
Afecta persistencia.
Afecta contratos externos.
Afecta múltiples módulos.
Es difícil de revertir.
Puede generar migraciones.
Condiciona la arquitectura futura.

requiere mayor análisis que una decisión local y fácilmente reversible.

24. Principio de consistencia

Una decisión válida debe poder coexistir con las decisiones existentes del
sistema.

Antes de aceptar una propuesta se debe preguntar:

¿Contradice alguna decisión anterior?
¿Introduce una nueva excepción?
¿Duplica conceptos existentes?
¿Rompe una frontera de módulo?
¿Cambia el significado de un concepto existente?
¿Obliga a modificar decisiones previamente aprobadas?

La consistencia global tiene prioridad sobre la conveniencia local.

25. Principio de explicitud ante conflictos

Cuando dos requisitos, documentos, decisiones o componentes entren en
conflicto, el conflicto debe hacerse explícito.

Claude no debe resolver silenciosamente el conflicto mediante una interpretación
arbitraria.

Debe informar:

CONFLICTO DETECTADO

Elemento A:
...

Elemento B:
...

Impacto:
...

Decisión requerida:
...
26. Principio de mejora continua

La metodología de desarrollo también puede evolucionar.

Si durante el proyecto se detecta que una regla, plantilla o proceso:

Es ambiguo.
Es innecesariamente burocrático.
No permite representar correctamente el dominio.
Genera inconsistencias.
Dificulta el trabajo.
No produce el resultado esperado.

Claude debe señalarlo y proponer una mejora.

Sin embargo, no debe modificar las reglas metodológicas silenciosamente.

Las modificaciones a .claude/ también forman parte de las decisiones del
proyecto y deben tratarse como tales.

Principio general

Ante cualquier decisión, Claude debe intentar responder:

¿Qué problema estamos resolviendo?
¿Qué sabemos con certeza?
¿Qué estamos suponiendo?
¿Qué alternativas existen?
¿Qué consecuencias tiene cada alternativa?
¿Qué riesgos introduce?
¿Qué tan reversible es?
¿Cómo afecta al resto del sistema?
¿Qué decisión requiere validación humana?

El objetivo no es encontrar siempre la solución más sofisticada.

El objetivo es encontrar una solución correcta, comprensible, justificable,
segura y suficientemente simple para el problema que realmente tenemos.