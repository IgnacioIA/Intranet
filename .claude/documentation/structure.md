# Documentation Structure

## 1. Propósito

Este documento define la estructura física de la documentación del proyecto.

Establece:

* Dónde vive cada tipo de artefacto.
* Qué responsabilidad tiene cada sección de `docs/`.
* Cómo se organiza la documentación del proyecto.
* Qué artefactos pueden coexistir dentro de un mismo documento.
* Qué contenido pertenece a `.claude/` y qué contenido pertenece a `docs/`.

Este documento define organización física. No redefine el significado, ciclo de vida o contenido conceptual de los artefactos, los cuales se encuentran definidos en los documentos especializados correspondientes.

---

## 2. Separación entre `.claude/` y `docs/`

El proyecto mantiene una separación explícita entre:

```text
.claude/
docs/
```

### `.claude/`

Contiene las reglas, metodologías, convenciones y criterios que deben utilizarse para trabajar sobre el proyecto.

Responde principalmente a:

> ¿Cómo debe trabajar Claude sobre este proyecto?

Ejemplos:

* Principios de ingeniería.
* Autoridad de decisiones.
* Metodología SDD.
* Reglas DDD.
* Principios arquitectónicos.
* Convenciones documentales.
* Estándares de desarrollo.
* Reglas de seguridad.

`.claude/` no debe utilizarse como repositorio de decisiones o requisitos específicos de Laucom.

---

### `docs/`

Contiene el conocimiento específico y durable del proyecto.

Responde principalmente a:

> ¿Qué estamos construyendo y cuáles son las decisiones que definen Laucom?

Ejemplos:

* Contexto del proyecto.
* Requisitos.
* Especificaciones.
* Modelo de dominio.
* Arquitectura adoptada.
* Decisiones arquitectónicas.
* Trazabilidad.

---

## 3. Estructura raíz

La estructura documental del proyecto es:

```text
docs/
├── README.md
├── PROJECT.md
├── GLOSSARY.md
│
├── 01-requirements/
│   ├── functional-requirements.md
│   └── non-functional-requirements.md
│
├── 02-domain/
│   ├── entities.md
│   ├── invariants.md
│   ├── states.md
│   └── transitions.md
│
├── 03-architecture/
│   ├── architecture.md
│   ├── infrastructure.md
│   └── security.md
│
├── 04-adr/
│
├── 05-traceability/
│   └── traceability.md
│
└── 06-specifications/
```

La carpeta `06-specifications/` contiene las especificaciones funcionales y sus casos de uso asociados.

No existe una carpeta independiente para casos de uso.

---

## 4. Documentación raíz

Los documentos directamente bajo `docs/` contienen información transversal necesaria para comprender el proyecto.

### `README.md`

Es el punto de entrada de la documentación.

Debe permitir identificar rápidamente:

* Qué contiene `docs/`.
* Cómo navegar por la documentación.
* Dónde encontrar cada tipo de artefacto.
* Qué documentos deben consultarse antes de trabajar sobre el proyecto.

No debe convertirse en un duplicado del contenido de las demás secciones.

---

### `PROJECT.md`

Describe el contexto general del proyecto.

Puede contener:

* Propósito del sistema.
* Alcance general.
* Problema que resuelve.
* Contexto del negocio.
* Objetivos generales.
* Restricciones generales.
* Estado general del proyecto.

No debe utilizarse para almacenar requisitos funcionales individuales ni especificaciones.

---

### `GLOSSARY.md`

Contiene el vocabulario oficial del proyecto.

Debe registrar términos relevantes del negocio, dominio y sistema cuando exista riesgo de ambigüedad.

Los términos utilizados en identificadores de contexto deben ser coherentes con este vocabulario.

El glosario no debe utilizarse para almacenar reglas de negocio completas.

---

# 5. `01-requirements/`

Contiene los requisitos aprobados o en proceso de definición del proyecto.

```text
01-requirements/
├── functional-requirements.md
└── non-functional-requirements.md
```

### `functional-requirements.md`

Contiene los requisitos funcionales del sistema.

Los requisitos individuales deben identificarse mediante IDs `REQ-<CONTEXTO>-<NUMERO>`.

Ejemplo:

```text
REQ-USR-001
REQ-USR-002
```

Los requisitos deben describir necesidades o resultados esperados, no detalles concretos de implementación.

---

### `non-functional-requirements.md`

Contiene requisitos relacionados con características de calidad, restricciones o condiciones no funcionales.

Ejemplos:

* Rendimiento.
* Disponibilidad.
* Seguridad.
* Auditabilidad.
* Mantenibilidad.
* Restricciones técnicas cuando correspondan como requisito.

Los requisitos no funcionales también utilizan IDs `REQ-<CONTEXTO>-<NUMERO>`.

La clasificación como funcional o no funcional no modifica el formato del identificador.

---

# 6. `02-domain/`

Contiene la documentación del modelo de dominio específico de Laucom.

```text
02-domain/
├── entities.md
├── invariants.md
├── states.md
└── transitions.md
```

Esta sección documenta el dominio que ha sido definido para el proyecto.

No debe utilizarse para inventariar automáticamente las clases Java existentes.

El modelo de dominio debe derivarse de las necesidades, reglas y conceptos del negocio, siguiendo los principios DDD definidos en `.claude/ddd/`.

---

### `entities.md`

Documenta las entidades del dominio que hayan sido identificadas como relevantes.

Debe evitar convertirse en una lista de tablas de base de datos.

---

### `invariants.md`

Documenta invariantes y reglas que deben mantenerse válidas en el dominio.

Cuando una invariante esté asociada a una SPEC concreta, debe existir una referencia explícita hacia ella.

---

### `states.md`

Documenta estados relevantes de conceptos del dominio cuando estos formen parte de su comportamiento.

No debe confundirse con:

* Estados del ciclo de vida de una SPEC.
* Estados de implementación.
* Estados operativos de infraestructura.

---

### `transitions.md`

Documenta transiciones relevantes entre estados del dominio.

Una transición debe indicar, cuando corresponda:

* Estado inicial.
* Evento o condición.
* Estado resultante.
* Reglas que permiten la transición.
* Restricciones relevantes.

---

# 7. `03-architecture/`

Contiene la arquitectura específica adoptada por Laucom.

```text
03-architecture/
├── architecture.md
├── infrastructure.md
└── security.md
```

Esta documentación representa decisiones y características reales del proyecto.

No debe contener reglas generales sobre cómo Claude debe diseñar sistemas. Esas reglas pertenecen a `.claude/architecture/`.

---

### `architecture.md`

Describe la arquitectura de aplicación adoptada.

Puede incluir:

* Estilo arquitectónico.
* Límites de módulos.
* Capas.
* Dependencias permitidas.
* Integraciones.
* Principios arquitectónicos específicos de Laucom.
* Componentes principales.

Las decisiones arquitectónicas significativas deben estar respaldadas por ADR cuando corresponda.

---

### `infrastructure.md`

Describe la infraestructura necesaria para ejecutar y operar el sistema.

Puede incluir:

* Servidores.
* Bases de datos.
* Servicios externos.
* Redes.
* Entornos.
* Despliegue.
* Dependencias de infraestructura.
* Requisitos operativos.

Las decisiones críticas de infraestructura deben seguir la autoridad de decisión definida en `.claude/core/decision-authority.md`.

---

### `security.md`

Contiene las decisiones y requisitos de seguridad específicos de Laucom.

Puede incluir:

* Autenticación.
* Autorización.
* Roles y permisos.
* Protección de datos.
* Auditoría.
* Gestión de credenciales.
* Amenazas relevantes.
* Controles de seguridad adoptados.

Las reglas generales que Claude debe seguir al razonar sobre seguridad pertenecen a `.claude/security/`.

---

# 8. `04-adr/`

Contiene las decisiones arquitectónicas formalmente registradas mediante Architecture Decision Records.

Cada ADR debe existir como documento independiente.

Ejemplo:

```text
04-adr/
├── ADR-001-arquitectura-inicial.md
├── ADR-002-persistencia.md
└── ADR-003-autenticacion.md
```

El formato y ciclo de vida de un ADR se define en:

```text
.claude/architecture/adr.md
```

Los ADR documentan decisiones, no requisitos ni especificaciones.

Una SPEC puede referenciar un ADR cuando su comportamiento dependa de una decisión arquitectónica relevante.

---

# 9. `05-traceability/`

Contiene la trazabilidad global del proyecto.

```text
05-traceability/
└── traceability.md
```

La trazabilidad permite relacionar, cuando corresponda:

```text
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
```

También puede relacionar decisiones arquitectónicas:

```text
SPEC
 ↓
ADR
```

El contenido de esta sección debe mantenerse como una vista de relaciones, no como una copia del contenido de los artefactos relacionados.

Las reglas de trazabilidad se encuentran en:

```text
.claude/sdd/traceability.md
```

---

# 10. `06-specifications/`

Esta sección contiene las especificaciones del sistema.

```text
06-specifications/
├── SPEC-XXX-NNN-descripcion.md
├── SPEC-XXX-NNN-descripcion.md
└── ...
```

Cada SPEC representa una capacidad o comportamiento coherente del sistema.

La definición conceptual, granularidad, contenido y ciclo de vida de una SPEC están definidos en:

```text
.claude/sdd/specification.md
.claude/sdd/lifecycle.md
```

---

## 10.1 Casos de uso dentro de una SPEC

Los casos de uso no tienen una carpeta independiente.

Cuando una SPEC requiere uno o más casos de uso, estos se documentan dentro de la propia SPEC.

Ejemplo:

```text
06-specifications/
└── SPEC-USR-001-crear-usuario.md
```

Contenido conceptual:

```text
SPEC-USR-001
│
├── Objetivo
├── Alcance
├── Requisitos relacionados
├── Actores
├── Reglas de negocio
│
├── UC-USR-001
│   ├── Actor
│   ├── Precondiciones
│   ├── Flujo principal
│   ├── Flujos alternativos
│   ├── Errores
│   └── Resultado esperado
│
├── Criterios de aceptación
├── Dominio involucrado
└── Trazabilidad
```

Una SPEC puede contener múltiples UC:

```text
SPEC-USR-001
├── UC-USR-001
└── UC-USR-002
```

No es obligatorio crear un UC cuando la capacidad especificada no requiere una interacción actor-sistema explícita.

---

# 11. Relación entre las secciones

Las secciones no representan una secuencia rígida de implementación.

Su relación conceptual es:

```text
01-requirements/
        │
        ▼
06-specifications/
        │
        ├── UC
        │
        ▼
02-domain/
        │
        ▼
03-architecture/
        │
        ▼
04-adr/
        │
        ▼
implementation
        │
        ▼
05-traceability/
```

La trazabilidad no implica que todos los artefactos deban relacionarse con todos los demás.

Las relaciones necesarias dependen de la naturaleza, complejidad y riesgo del cambio.

---

# 12. No duplicación de información

Cada tipo de información debe tener una ubicación principal.

No debe copiarse una misma definición completa en múltiples documentos para mantener consistencia manualmente.

Ejemplo incorrecto:

```text
SPEC-USR-001
    contiene una regla

02-domain/invariants.md
    contiene una copia completa de la misma regla

05-traceability/traceability.md
    contiene otra copia de la misma regla
```

Preferir:

```text
SPEC-USR-001
    define/referencia la regla

02-domain/invariants.md
    documenta la invariante del dominio

05-traceability/traceability.md
    relaciona ambos artefactos
```

Las relaciones deben expresarse mediante identificadores.

---

# 13. Documentación vs. implementación

La estructura de `docs/` documenta el sistema, pero no reemplaza al código.

Por ejemplo:

```text
docs/02-domain/entities.md
```

documenta el modelo conceptual.

La implementación concreta puede encontrarse en:

```text
src/main/java/...
```

Una diferencia entre documentación e implementación debe investigarse y no resolverse automáticamente modificando la documentación para hacerla coincidir con el código.

La jerarquía de fuentes de verdad se encuentra definida en `.claude/CLAUDE.md`.

---

# 14. Documentación temporal y análisis

No todo análisis realizado durante el desarrollo debe convertirse automáticamente en documentación permanente.

Notas temporales, exploraciones, alternativas descartadas y análisis de trabajo pueden mantenerse fuera de la estructura permanente cuando no representen conocimiento necesario para comprender el sistema.

Una vez tomada una decisión relevante, debe conservarse únicamente la información necesaria para comprender:

* Qué se decidió.
* Por qué.
* Qué consecuencias tiene.
* Qué artefactos afecta.

Cuando corresponda, esta información debe registrarse mediante ADR o en el artefacto documental apropiado.

---

# 15. Evolución de la estructura

La estructura documental puede evolucionar.

Agregar una nueva categoría de documentación debe justificarse cuando:

* El volumen de información lo requiera.
* Exista una nueva clase de artefacto.
* La separación mejore significativamente la navegación.
* Sea necesario para mantener trazabilidad o claridad.

No debe crearse una carpeta únicamente para satisfacer una clasificación teórica si la cantidad de documentación no lo justifica.

Los cambios estructurales que afecten referencias existentes deben analizarse antes de aplicarse.

---

# 16. Regla de mínima estructura

La estructura documental debe ser tan simple como sea posible sin perder:

* Claridad.
* Trazabilidad.
* Separación de responsabilidades.
* Navegabilidad.
* Capacidad de evolución.

La existencia de una carpeta no implica que deba llenarse inmediatamente.

La estructura puede anticipar artefactos futuros, pero no debe utilizarse como justificación para crear documentación que todavía no tiene contenido significativo.

---

# 17. Resumen

La documentación específica del proyecto se organiza de la siguiente manera:

```text
docs/
│
├── README.md
├── PROJECT.md
├── GLOSSARY.md
│
├── 01-requirements/
│   ├── functional-requirements.md
│   └── non-functional-requirements.md
│
├── 02-domain/
│   ├── entities.md
│   ├── invariants.md
│   ├── states.md
│   └── transitions.md
│
├── 03-architecture/
│   ├── architecture.md
│   ├── infrastructure.md
│   └── security.md
│
├── 04-adr/
│
├── 05-traceability/
│   └── traceability.md
│
└── 06-specifications/
    └── SPEC-XXX-NNN-descripcion.md
```

La relación principal de los artefactos SDD es:

```text
REQ
 ↓
SPEC
 ├── UC
 ↓
DOMAIN
 ↓
CODE
 ↓
TEST
```

Los casos de uso forman parte de las especificaciones y no poseen actualmente una ubicación documental independiente.

La estructura física definida aquí debe mantenerse separada de las reglas metodológicas contenidas en `.claude/`.
