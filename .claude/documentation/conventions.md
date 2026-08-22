# Documentation Conventions

## 1. Propósito

Este documento define las convenciones utilizadas para identificar, nombrar, referenciar y organizar los artefactos documentales del proyecto.

Su objetivo es garantizar:

* Identificación única de los artefactos.
* Referencias consistentes entre documentos.
* Trazabilidad entre necesidad, especificación, dominio, implementación y pruebas.
* Nombres de archivos predecibles.
* Separación clara entre documentación metodológica y documentación específica del proyecto.

Este documento define convenciones de documentación. No redefine el significado, ciclo de vida o contenido conceptual de cada artefacto cuando estos aspectos ya están definidos en otros documentos.

Las reglas metodológicas de SDD se encuentran en `.claude/sdd/`.

---

## 2. Fuente de autoridad

Las convenciones de este documento deben interpretarse junto con:

* `.claude/CLAUDE.md`
* `.claude/core/`
* `.claude/sdd/`
* `.claude/documentation/structure.md`
* `.claude/documentation/templates.md`

Cuando exista una contradicción entre documentos, la contradicción debe señalarse explícitamente y no resolverse silenciosamente.

Las decisiones específicas del proyecto Laucom no deben incorporarse a este documento salvo que constituyan una convención general aplicable a sus artefactos documentales.

---

## 3. Principios de identificación

Todo artefacto que requiera trazabilidad debe poseer un identificador estable y único.

Un identificador:

* Debe ser único dentro de su tipo de artefacto.
* No debe reutilizarse después de que el artefacto haya sido retirado, rechazado o reemplazado.
* Debe permanecer estable durante la evolución del artefacto.
* Debe utilizarse en referencias entre documentos.
* No debe depender del nombre del archivo para mantener su identidad.

El nombre de un archivo puede cambiar sin que esto implique necesariamente un cambio en el identificador del artefacto.

---

## 4. Formato general de identificadores

Los identificadores siguen el siguiente patrón conceptual:

```text
<TIPO>-<CONTEXTO>-<NUMERO>
```

Ejemplo:

```text
REQ-USR-001
SPEC-USR-001
UC-USR-001
ADR-001
TEST-USR-001
```

Donde:

* `TIPO` identifica la clase de artefacto.
* `CONTEXTO` identifica el contexto funcional o conceptual cuando corresponda.
* `NUMERO` identifica de forma única el artefacto dentro de su contexto.

El contexto debe utilizar únicamente vocabulario conocido y definido por el proyecto.

No debe inventarse un contexto únicamente para satisfacer el formato del identificador.

---

## 5. Tipos de identificadores

### 5.1 Requisitos

Formato:

```text
REQ-<CONTEXTO>-<NUMERO>
```

Ejemplo:

```text
REQ-USR-001
```

Los requisitos funcionales y no funcionales utilizan el mismo esquema de identificación.

La clasificación como requisito funcional o no funcional se determina por el contenido y ubicación documental del requisito, no mediante un prefijo diferente.

---

### 5.2 Especificaciones

Formato:

```text
SPEC-<CONTEXTO>-<NUMERO>
```

Ejemplo:

```text
SPEC-USR-001
```

Una SPEC representa una capacidad o comportamiento coherente del sistema.

La granularidad de las especificaciones está definida en `.claude/sdd/specification.md`.

No debe crearse una SPEC por clase, método o detalle de implementación.

Tampoco debe utilizarse una única SPEC para representar arbitrariamente todo un módulo cuando existan capacidades independientes.

---

### 5.3 Casos de uso

Formato:

```text
UC-<CONTEXTO>-<NUMERO>
```

Ejemplo:

```text
UC-USR-001
```

Los casos de uso representan interacciones orientadas a un objetivo entre un actor y el sistema.

Los UC forman parte de una SPEC cuando corresponda.

No existe una obligación de correspondencia numérica entre una SPEC y sus UC.

Por ejemplo, es válido:

```text
SPEC-USR-001
├── UC-USR-001
└── UC-USR-002
```

También es válido que una SPEC tenga un único UC:

```text
SPEC-USR-002
└── UC-USR-003
```

El número del UC identifica al caso de uso, no su posición dentro de una SPEC.

---

### 5.4 Decisiones arquitectónicas

Formato:

```text
ADR-<NUMERO>
```

Ejemplo:

```text
ADR-001
ADR-002
```

Los ADR representan decisiones arquitectónicas o técnicas que requieren registro formal según las reglas de autoridad de decisión del proyecto.

El mecanismo y contenido de los ADR se define en:

```text
.claude/architecture/adr.md
```

---

### 5.5 Pruebas

Cuando una prueba requiera identificación documental explícita para trazabilidad, utilizará:

```text
TEST-<CONTEXTO>-<NUMERO>
```

Ejemplo:

```text
TEST-USR-001
```

El identificador conceptual de una prueba no obliga a que exista un archivo documental independiente para cada prueba.

La implementación concreta de las pruebas se encuentra en el código y sigue las convenciones definidas en `.claude/development/testing.md`.

---

## 6. Relación entre identificadores

Los identificadores representan artefactos independientes.

No debe asumirse una relación 1:1 únicamente porque dos identificadores compartan contexto o número.

Por ejemplo:

```text
REQ-USR-001
    ↓
SPEC-USR-001
    ↓
UC-USR-001
    ↓
TEST-USR-004
```

es una relación válida, pero el hecho de compartir `USR-001` no constituye por sí mismo una regla de correspondencia.

Las relaciones reales entre artefactos deben expresarse mediante referencias explícitas.

La relación de trazabilidad se encuentra definida en:

```text
.claude/sdd/traceability.md
```

---

## 7. Relación SPEC → UC

Una SPEC puede contener:

* Ningún UC, cuando el comportamiento no requiere una interacción actor-sistema explícita.
* Un UC.
* Múltiples UC.

Cuando existan UC, estos forman parte de la SPEC a la que pertenecen.

Ejemplo:

```text
SPEC-USR-001
    ├── UC-USR-001
    └── UC-USR-002
```

Los UC no se consideran actualmente una categoría documental independiente en la estructura física de `docs/`.

No debe crearse una carpeta independiente `use-cases/` salvo que una decisión futura modifique explícitamente esta convención.

---

## 8. Nombres de archivos

Los archivos que representan artefactos identificables deben utilizar el identificador del artefacto como parte principal de su nombre.

Formato recomendado:

```text
<ID>-<descripcion-corta>.md
```

Ejemplos:

```text
REQ-USR-001-crear-usuario.md
SPEC-USR-001-crear-usuario.md
ADR-001-arquitectura-inicial.md
```

La descripción del archivo:

* Debe ser breve.
* Debe facilitar la navegación humana.
* No forma parte de la identidad del artefacto.
* Puede cambiar si mejora la claridad sin cambiar el ID.

Los nombres de archivo deben utilizar:

* minúsculas para la descripción;
* palabras separadas por guiones;
* caracteres ASCII cuando sea posible;
* ningún espacio;
* ningún carácter innecesario de puntuación.

---

## 9. Referencias entre documentos

Las referencias entre artefactos deben utilizar su identificador.

Ejemplo:

```text
Relacionado con: REQ-USR-001
```

o:

```text
Requisitos relacionados:
- REQ-USR-001
- REQ-USR-004
```

No debe utilizarse únicamente el nombre del archivo como referencia conceptual.

Esto permite cambiar el nombre o ubicación física de un archivo sin perder la identidad del artefacto.

---

## 10. Contextos utilizados en identificadores

El segmento `<CONTEXTO>` debe representar un contexto funcional o conceptual reconocido por el proyecto.

Ejemplos hipotéticos:

```text
USR
AUTH
COM
INV
```

No deben introducirse códigos de contexto arbitrarios.

Cuando el contexto todavía no esté definido, debe señalarse como GAP y resolverse antes de crear artefactos que dependan de él.

El vocabulario oficial del proyecto debe mantenerse en `docs/GLOSSARY.md`.

---

## 11. Unicidad y numeración

La numeración comienza en `001` para cada tipo y contexto.

Ejemplo:

```text
SPEC-USR-001
SPEC-USR-002
SPEC-USR-003
```

No se deben reutilizar números de artefactos eliminados o reemplazados.

Si:

```text
SPEC-USR-002
```

queda `SUPERSEDED`, el identificador no vuelve a estar disponible para una nueva especificación.

La existencia de saltos numéricos no constituye un problema.

Ejemplo válido:

```text
SPEC-USR-001
SPEC-USR-003
SPEC-USR-004
```

El número `002` puede haber correspondido a un artefacto posteriormente rechazado, reemplazado o retirado.

---

## 12. Estados y ciclo de vida

Los identificadores no codifican el estado de un artefacto.

No deben utilizarse nombres como:

```text
SPEC-USR-001-DRAFT
SPEC-USR-001-APPROVED
SPEC-USR-001-FINAL
```

El estado se mantiene como metadata del artefacto y sigue el ciclo de vida definido por `.claude/sdd/lifecycle.md`.

Por lo tanto:

```text
SPEC-USR-001
```

mantiene su identidad mientras atraviesa sus distintos estados.

---

## 13. Versionado

La evolución de un artefacto no requiere modificar su identificador.

Cuando el versionado explícito sea necesario, se utilizará metadata del documento.

Ejemplo:

```text
ID: SPEC-USR-001
Version: 1.2
Status: APPROVED
```

El formato concreto de metadata y versionado debe mantenerse consistente con `.claude/documentation/templates.md`.

No debe utilizarse el número de versión como sustituto del identificador.

---

## 14. Fechas y autoría

Las fechas y autores deben registrarse únicamente cuando aporten valor para comprender el historial o la responsabilidad de un artefacto.

No deben utilizarse como parte del identificador.

No debe asumirse que la persona que escribe un documento es necesariamente la autoridad que aprobó la decisión contenida en él.

La autoridad de decisión se determina según `.claude/core/decision-authority.md`.

---

## 15. Artefactos rechazados, reemplazados o retirados

Un artefacto que haya existido formalmente conserva su identificador aunque deje de estar vigente.

Cuando corresponda, debe indicarse su relación con el artefacto que lo reemplaza.

Ejemplo:

```text
SPEC-USR-001
Status: SUPERSEDED
Superseded by: SPEC-USR-004
```

No debe eliminarse silenciosamente un artefacto cuya existencia sea relevante para comprender la evolución del sistema.

---

## 16. Trazabilidad

Los identificadores deben permitir recorrer la trazabilidad conceptual del sistema.

La cadena canónica es:

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

No todos los artefactos requieren necesariamente todos los eslabones de la cadena.

La necesidad y profundidad de trazabilidad dependen de la complejidad, criticidad y riesgo del cambio, según las reglas definidas en `.claude/sdd/traceability.md`.

---

## 17. Qué NO debe resolverse mediante convenciones

Las convenciones no deben utilizarse para decidir:

* Qué funcionalidades tendrá Laucom.
* Qué entidades posee el dominio.
* Qué arquitectura utilizará el sistema.
* Qué reglas de negocio existen.
* Qué tecnologías deben utilizarse.
* Qué permisos tendrá un usuario.
* Qué requisitos son prioritarios.
* Qué decisiones arquitectónicas deben adoptarse.

Esas cuestiones pertenecen a los artefactos correspondientes y deben seguir la autoridad de decisión definida por el proyecto.

---

## 18. Regla de mínima complejidad documental

Las convenciones deben facilitar el desarrollo y la trazabilidad, no convertirse en una carga administrativa innecesaria.

No debe crearse un artefacto documental únicamente para cumplir una convención si el comportamiento o decisión no requiere dicho nivel de formalización.

La cantidad y profundidad de documentación debe ser proporcional al impacto, complejidad, riesgo y criticidad del cambio.

Esta regla no elimina las obligaciones explícitas establecidas por SDD para funcionalidades que requieran especificación formal.

---

## 19. Cambios a estas convenciones

Una modificación de estas convenciones debe analizarse antes de aplicarse cuando pueda afectar:

* Identificadores existentes.
* Referencias entre artefactos.
* Trazabilidad.
* Estructura documental.
* Automatizaciones.
* Herramientas que dependan de los formatos definidos.

Los cambios que afecten artefactos existentes no deben realizarse mediante modificaciones silenciosas.

Cuando corresponda, debe registrarse la decisión y analizarse el impacto sobre los documentos existentes.

---

## 20. Resumen de convenciones

| Artefacto               | Identificador              | Ejemplo        |
| ----------------------- | -------------------------- | -------------- |
| Requisito               | `REQ-<CONTEXTO>-<NUMERO>`  | `REQ-USR-001`  |
| Especificación          | `SPEC-<CONTEXTO>-<NUMERO>` | `SPEC-USR-001` |
| Caso de uso             | `UC-<CONTEXTO>-<NUMERO>`   | `UC-USR-001`   |
| Decisión arquitectónica | `ADR-<NUMERO>`             | `ADR-001`      |
| Prueba trazable         | `TEST-<CONTEXTO>-<NUMERO>` | `TEST-USR-001` |

### Relación principal

```text
REQ
 ↓
SPEC
 ├── UC
 ├── reglas
 ├── criterios de aceptación
 └── dominio involucrado
 ↓
DOMAIN
 ↓
CODE
 ↓
TEST
```

Las especificaciones constituyen la unidad central de comportamiento del proceso SDD.

Los casos de uso, cuando corresponda, forman parte de la especificación y representan las interacciones necesarias para materializar el comportamiento especificado.
