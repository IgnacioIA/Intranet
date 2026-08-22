# Architecture Decision Records

## 1. Propósito

Este documento define el mecanismo utilizado para registrar y gestionar decisiones arquitectónicas del proyecto mediante Architecture Decision Records (ADR).

Su objetivo es preservar:

- la decisión adoptada;
- el contexto que la originó;
- las alternativas consideradas;
- el razonamiento utilizado;
- las consecuencias de la decisión;
- las relaciones con otras decisiones y artefactos del proyecto.

Este documento define el mecanismo de ADR.

No contiene decisiones arquitectónicas específicas del proyecto.

Las decisiones concretas deben registrarse en la documentación del proyecto, utilizando la estructura definida en `.claude/documentation/structure.md`.

---

# 2. Relación con Decision Authority

La autoridad para tomar una decisión y el mecanismo para documentarla son conceptos diferentes.

`.claude/core/decision-authority.md` define:

- quién tiene autoridad para tomar decisiones;
- los niveles de decisión;
- cuándo una decisión debe escalarse;
- qué decisiones requieren intervención humana;
- cómo se determina la autoridad sobre una decisión.

Este documento define:

- cuándo una decisión debe registrarse mediante ADR;
- qué información debe contener el ADR;
- cómo se relacionan los ADR;
- cómo evoluciona el estado de una decisión.

Por lo tanto:

```text
Decision Authority
        │
        │ determina
        ▼
¿Quién puede decidir?
¿Qué nivel tiene?
¿Debe escalarse?
        │
        ▼
Architecture Decision Record
        │
        │ registra
        ▼
¿Qué se decidió?
¿Por qué?
¿Qué consecuencias tiene?

Un ADR no otorga autoridad para tomar una decisión.

Registrar una decisión no sustituye el proceso de autorización establecido en decision-authority.md.

3. ¿Qué es un ADR?

Un Architecture Decision Record es un registro permanente de una decisión arquitectónica significativa.

Una decisión arquitectónica es una decisión que establece, modifica o restringe una característica relevante de la estructura, comportamiento técnico o evolución del sistema.

Un ADR debe permitir que una persona que no participó en la decisión pueda comprender:

qué problema existía;
qué alternativas fueron consideradas;
qué decisión se tomó;
por qué se tomó;
qué consecuencias genera;
qué partes del sistema quedan afectadas.

Un ADR registra una decisión.

No es:

una especificación funcional;
una especificación de implementación;
una guía de programación;
documentación de código;
una explicación general de una tecnología;
un reemplazo de una Specification;
un reemplazo de la documentación de dominio.
4. ¿Cuándo se requiere un ADR?

Debe utilizarse un ADR cuando una decisión tenga suficiente impacto como para que perder su contexto o razonamiento pueda generar problemas futuros.

Son candidatos a ADR las decisiones que:

afectan la estructura general del sistema;
establecen límites entre módulos o componentes;
determinan una estrategia de integración;
establecen una tecnología o mecanismo arquitectónicamente relevante;
introducen una restricción técnica significativa;
afectan múltiples partes del sistema;
establecen una política que condicionará decisiones futuras;
tienen consecuencias importantes de mantenimiento o evolución;
generan un compromiso técnico significativo;
modifican una decisión arquitectónica previamente registrada;
pueden producir desacuerdo futuro y requieren conservar el razonamiento que llevó a la decisión.

Ejemplos conceptuales:

elección de una estrategia de comunicación entre módulos;
definición de límites arquitectónicos;
adopción de una estrategia de persistencia que afecte la arquitectura;
decisión sobre una estrategia de integración externa;
adopción de una restricción técnica que condicione múltiples componentes;
reemplazo de una decisión arquitectónica existente.
5. ¿Cuándo NO se requiere un ADR?

No toda decisión técnica necesita un ADR.

No deben crearse ADR para:

decisiones triviales;
decisiones locales de implementación;
decisiones de estilo de código;
nombres de variables, métodos o clases;
refactorizaciones internas sin impacto arquitectónico;
decisiones que ya estén completamente determinadas por una decisión arquitectónica existente;
detalles de implementación que no afecten la arquitectura;
documentación de comportamiento que corresponde a una Specification;
reglas de negocio que corresponden a requisitos o dominio.

Ejemplo:

"Usar una clase de servicio para encapsular esta operación"

normalmente no requiere ADR.

En cambio:

"Todas las capacidades del sistema deberán exponerse mediante
una capa de aplicación independiente del dominio"

puede requerir ADR porque establece una regla arquitectónica que afecta múltiples partes del sistema.

6. Criterio de significatividad

La decisión de crear un ADR debe considerar el impacto de la decisión y no únicamente su dificultad técnica.

Como regla práctica, una decisión es candidata a ADR cuando cumple una o más de las siguientes condiciones:

afecta más de un módulo;
afecta una frontera arquitectónica;
condiciona futuras decisiones;
introduce una dependencia importante;
es difícil o costosa de revertir;
afecta seguridad;
afecta integración;
afecta persistencia a nivel arquitectónico;
afecta infraestructura;
modifica una decisión previamente documentada;
tiene consecuencias relevantes para la evolución del sistema.

Cuando exista duda razonable sobre si una decisión requiere ADR, debe preferirse registrar la decisión antes que perder su contexto.

7. ADR y decisiones no arquitectónicas

No todas las decisiones importantes pertenecen a un ADR.

La clasificación debe realizarse antes de crear el documento.

Ejemplos:

Tipo de decisión	Artefacto principal
Necesidad del negocio	Requirement
Comportamiento de una capacidad	Specification
Regla del dominio	Documentación de dominio
Decisión arquitectónica	ADR
Decisión de implementación local	Código / documentación técnica correspondiente
Cambio de contrato externo	SPEC / API Contract y trazabilidad correspondiente
Decisión sobre seguridad	Según su naturaleza y autoridad
Decisión de proceso SDD	.claude/ correspondiente

Un ADR puede relacionarse con otros tipos de artefactos, pero no debe reemplazarlos.

8. Estados de un ADR

Un ADR puede encontrarse en los siguientes estados:

Propuesto

La decisión está siendo evaluada y todavía no ha sido adoptada.

Un ADR en estado Propuesto documenta una decisión que está en discusión.

No debe tratarse como una regla vigente del sistema.

Aceptado

La decisión fue aprobada por la autoridad correspondiente y constituye una decisión vigente.

Las implementaciones y decisiones posteriores deben respetarla mientras continúe vigente.

Rechazado

La alternativa documentada no fue adoptada.

El ADR puede conservarse para preservar el razonamiento y evitar volver a analizar innecesariamente la misma alternativa.

Supersedido

La decisión fue reemplazada por otra decisión posterior.

El ADR debe referenciar el ADR que lo reemplaza.

Deprecado

La decisión dejó de ser aplicable, pero no necesariamente fue reemplazada por una nueva decisión.

El motivo debe quedar documentado.

9. Estado y autoridad

El estado de un ADR no determina quién tiene autoridad para cambiarlo.

Una transición de estado debe respetar las reglas de autoridad establecidas en:

.claude/core/decision-authority.md

En particular:

Claude no debe convertir una decisión propuesta en aceptada si no posee autoridad para hacerlo.
Claude no debe asumir que una decisión técnica es automáticamente aprobable.
Una decisión que requiera intervención humana debe detener el proceso hasta obtener la decisión correspondiente.
El ADR debe registrar quién o qué autoridad tomó la decisión cuando esa información sea relevante para el proyecto.
10. Estructura obligatoria de un ADR

Todo ADR debe contener, como mínimo:

Identificador.
Título.
Estado.
Fecha.
Contexto.
Decisión.
Alternativas consideradas cuando existan alternativas relevantes.
Consecuencias.
Áreas afectadas.
Referencias a documentación relacionada cuando corresponda.

La estructura material de la plantilla se encuentra definida en:

.claude/documentation/templates.md

Este documento define las reglas.

templates.md define la forma de escribirlas.

11. Contexto

La sección de contexto debe explicar el problema que hizo necesaria la decisión.

Debe responder, cuando corresponda:

¿Qué situación existe?
¿Qué problema debe resolverse?
¿Qué restricciones existen?
¿Qué motivó la necesidad de decidir?
¿Qué consecuencias tendría no tomar una decisión?

El contexto debe describir la situación anterior a la decisión.

No debe utilizarse para justificar retrospectivamente una decisión ya tomada.

12. Decisión

La sección de decisión debe expresar claramente la alternativa adoptada.

Debe poder responderse de forma inequívoca:

¿Qué se decidió?

Debe evitar expresiones ambiguas como:

"se recomienda";
"probablemente";
"se podría";
"se evaluará".

Si el ADR está en estado Aceptado, la sección debe representar una decisión concreta.

13. Alternativas consideradas

Cuando existan alternativas relevantes, deben documentarse.

No es necesario registrar todas las posibilidades imaginables.

Deben registrarse las alternativas que:

hayan sido seriamente consideradas;
sean técnicamente viables;
hayan influido en la decisión;
permitan comprender por qué se descartaron otras opciones.

El objetivo no es demostrar que una alternativa era "incorrecta".

El objetivo es preservar el razonamiento utilizado para elegir la alternativa adoptada.

14. Consecuencias

Toda decisión arquitectónica genera consecuencias.

Deben documentarse cuando corresponda:

Consecuencias positivas

Beneficios obtenidos por la decisión.

Consecuencias negativas

Costos, limitaciones o compromisos introducidos.

Riesgos

Problemas potenciales que deben ser monitoreados.

No debe asumirse que una decisión arquitectónica es completamente positiva.

Un ADR debe hacer visibles los compromisos técnicos introducidos por la decisión.

15. Alcance de un ADR

Un ADR debe tener un alcance claramente definido.

Debe indicar qué partes del sistema quedan afectadas.

Puede afectar:

módulos;
componentes;
infraestructura;
integraciones;
persistencia;
seguridad;
procesos técnicos;
capacidades del sistema.

El ADR no debe extenderse innecesariamente hacia áreas que no están afectadas por la decisión.

16. Relación entre ADR

Los ADR pueden relacionarse entre sí.

Las relaciones principales son:

Complementa

Una decisión amplía o complementa otra decisión existente sin reemplazarla.

Depende de

Una decisión requiere que otra decisión permanezca vigente.

Contradice

Una nueva decisión entra en conflicto con una decisión existente y requiere resolver la contradicción.

Supersede

Una nueva decisión reemplaza una decisión anterior.

Cuando una decisión supersede otra, ambos ADR deben mantener referencias cruzadas.

Ejemplo:

ADR-005 — Estrategia de integración
        │
        │ superseded by
        ▼
ADR-012 — Nueva estrategia de integración

El ADR anterior debe pasar a estado Supersedido.

El nuevo ADR debe indicar explícitamente qué decisión reemplaza.

17. Modificación de decisiones existentes

Una decisión Aceptada no debe editarse para cambiar silenciosamente su significado.

Cuando la decisión cambie de manera significativa:

debe crearse un nuevo ADR;
debe documentarse el contexto del cambio;
debe indicarse qué ADR anterior queda afectado;
el ADR anterior debe pasar a Supersedido o Deprecado, según corresponda;
el nuevo ADR debe registrar la nueva decisión.

Esto permite conservar el historial de decisiones.

Los cambios editoriales que no alteren el significado de una decisión pueden realizarse sobre el ADR existente.

18. ADR y Specifications

Una decisión arquitectónica puede afectar una o más Specifications.

En ese caso:

ADR
 │
 ├── afecta → SPEC
 └── puede originarse por → REQ

La relación debe documentarse mediante referencias.

Un ADR no debe copiar el contenido de la Specification.

Por ejemplo:

SPEC:
Define qué debe hacer una capacidad.


ADR:
Define una decisión arquitectónica necesaria para soportar
esa capacidad.

La Specification describe el comportamiento requerido.

El ADR registra la decisión arquitectónica que condiciona su implementación.

19. ADR y Domain

Una decisión arquitectónica puede afectar al dominio, pero no debe utilizarse para definir directamente reglas de negocio.

Si una decisión afecta conceptos de dominio:

el ADR debe registrar la decisión arquitectónica;
la documentación de dominio debe registrar los conceptos y reglas correspondientes;
ambos deben relacionarse mediante trazabilidad.

No debe duplicarse el modelo de dominio dentro del ADR.

20. ADR y API Contract

Un API Contract pertenece a una Specification cuando una capacidad se expone mediante una API.

Una decisión arquitectónica relacionada con APIs puede requerir un ADR.

Estas dos cosas no deben confundirse.

SPEC
 └── API Contract
       ↓
     define la frontera externa


ADR
 └── decisión arquitectónica
       ↓
     define una decisión estructural o técnica
     que puede afectar esa frontera

Ejemplo conceptual:

API Contract:
GET /clientes/{id}


ADR:
Decisión sobre la estrategia arquitectónica
utilizada para exponer las capacidades mediante APIs.

El ADR no reemplaza al API Contract.

21. ADR y seguridad

Las decisiones relacionadas con seguridad pueden requerir ADR cuando tengan impacto arquitectónico significativo.

Ejemplos:

estrategia general de autenticación;
arquitectura de autorización;
gestión centralizada de identidad;
segmentación de servicios;
estrategia de secretos;
arquitectura de auditoría de seguridad.

Una regla de seguridad específica de una capacidad no debe convertirse automáticamente en un ADR.

La necesidad de ADR debe evaluarse según el impacto de la decisión.

Las reglas de seguridad del proyecto y su ubicación normativa deben resolverse mediante el mecanismo correspondiente.

22. ADR y tecnología

La adopción de una tecnología no requiere automáticamente un ADR.

Debe evaluarse el impacto de la decisión.

Ejemplo:

"No se utilizará una librería para resolver una tarea local."

Normalmente no requiere ADR.

En cambio:

"El sistema utilizará una determinada tecnología como
mecanismo central de persistencia."

puede requerir ADR si afecta la arquitectura y condiciona decisiones futuras.

La existencia de un ADR no debe utilizarse como excusa para documentar cada dependencia del proyecto.

23. ADR y reversibilidad

La dificultad de revertir una decisión es un factor importante para determinar si debe documentarse.

Una decisión:

fácil de revertir;
localizada;
de bajo impacto;

normalmente no requiere ADR.

Una decisión:

costosa de revertir;
transversal;
con múltiples dependencias;
con impacto sobre consumidores externos;

es una candidata fuerte a ADR.

La reversibilidad no es el único criterio, pero debe considerarse.

24. ADR y trazabilidad

Cuando corresponda, un ADR debe mantener referencias hacia:

Requirements;
Specifications;
Domain;
otros ADR;
pruebas o validaciones relevantes.

La relación no implica que todos los ADR deban estar vinculados a todos estos artefactos.

Solo deben registrarse las relaciones reales.

La trazabilidad debe seguir las reglas definidas en:

.claude/sdd/traceability.md

25. ADR huérfanos

Un ADR no debe mantener referencias hacia artefactos inexistentes.

Las referencias a Requirements, Specifications, Domain u otros ADR deben corresponder a artefactos reales.

Un ADR puede existir sin relación con una Specification cuando la decisión arquitectónica sea transversal y no derive de una capacidad específica.

26. Creación de un ADR durante la implementación

Si durante la implementación aparece una decisión arquitectónica que no estaba previamente documentada:

detener la implementación de la parte afectada;
identificar la decisión que debe resolverse;
determinar su nivel de autoridad según decision-authority.md;
solicitar la decisión correspondiente si requiere intervención humana;
registrar la decisión mediante un ADR cuando corresponda;
actualizar las referencias afectadas;
continuar la implementación únicamente cuando la decisión esté suficientemente resuelta.

Claude no debe ocultar una decisión arquitectónica dentro del código.

27. Decisiones no resueltas

Si una decisión arquitectónica relevante permanece abierta, no debe documentarse como Aceptada.

Puede registrarse como Propuesta cuando sea útil preservar el problema y las alternativas consideradas.

Una Specification no debe considerarse suficientemente definida si depende de una decisión arquitectónica crítica que todavía permanece sin resolver.

La evaluación del estado de la Specification debe seguir las reglas de .claude/sdd/lifecycle.md.

28. Revisión de ADR

Un ADR debe revisarse cuando:

cambie una decisión relacionada;
aparezca nueva información relevante;
cambien restricciones arquitectónicas;
una decisión deje de ser aplicable;
una decisión sea reemplazada;
una implementación revele consecuencias no previstas importantes.

La revisión debe preservar el historial.

No deben modificarse retrospectivamente las decisiones aceptadas para ocultar decisiones anteriores.

29. Checklist de calidad

Antes de considerar un ADR correctamente documentado, verificar:

 Tiene un identificador válido.
 Tiene un título claro.
 Tiene un estado definido.
 La autoridad de la decisión es compatible con decision-authority.md.
 El contexto explica por qué fue necesario decidir.
 La decisión está expresada de forma inequívoca.
 Las alternativas relevantes fueron consideradas.
 Las consecuencias están documentadas.
 Los riesgos relevantes están documentados.
 Las áreas afectadas están identificadas.
 Las relaciones con otros artefactos están documentadas cuando corresponda.
 No duplica una Specification.
 No duplica documentación de dominio.
 No introduce detalles de implementación innecesarios.
 No contiene decisiones inventadas.
 No modifica silenciosamente una decisión anterior.
 Las referencias utilizadas existen.
 La ubicación física respeta documentation/structure.md.
30. Principio general

Los ADR deben responder una pregunta concreta:

¿Qué decisión arquitectónica se tomó, por qué se tomó y qué consecuencias tiene?

El objetivo no es producir la mayor cantidad posible de documentación.

El objetivo es evitar que decisiones arquitectónicas significativas queden:

implícitas;
dispersas;
ocultas en el código;
olvidadas;
reinterpretadas posteriormente sin conocer su contexto.

Un ADR debe preservar el razonamiento de una decisión para que el proyecto pueda evolucionar sin perder la memoria de por qué su arquitectura es como es.