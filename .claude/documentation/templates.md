## 1. Propósito

Este documento define las plantillas estándar utilizadas para crear la documentación del proyecto dentro de `docs/`.

Su propósito es establecer cómo se estructuran y materializan los distintos artefactos documentales, sin duplicar las reglas conceptuales definidas por el modelo SDD, arquitectura y dominio.

Las reglas normativas de cada artefacto permanecen definidas en sus respectivos documentos de autoridad.

Este documento define la forma de materializar dichos artefactos.

---

## 2. Reglas generales

Toda documentación del proyecto debe respetar las siguientes reglas:

1. Utilizar la plantilla correspondiente al tipo de artefacto que se está creando.
2. No crear un nuevo tipo de artefacto documental sin establecer previamente su propósito y relación con el modelo SDD.
3. No duplicar la misma fuente de verdad en múltiples documentos.
4. Preferir referencias entre documentos antes que duplicar información.
5. La documentación debe poder comprenderse sin conocer detalles de implementación, salvo que el artefacto específicamente documente implementación.
6. Utilizar las convenciones de identificación definidas en `.claude/documentation/conventions.md`.
7. Respetar la estructura física de documentación definida en `.claude/documentation/structure.md`.
8. Las plantillas definen la estructura de los documentos; no reemplazan las reglas normativas definidas en `.claude/sdd/`.
9. Las secciones opcionales solo deben incluirse cuando correspondan.
10. No completar una sección con información inventada únicamente para satisfacer la plantilla.
11. Una plantilla es una guía estructural, no una obligación de que todas sus secciones tengan contenido.
12. Cuando una sección no sea aplicable, debe omitirse o indicarse explícitamente como "No aplica", según corresponda al tipo de documento.
13. La documentación debe mantener una única fuente de verdad para cada concepto.
14. Las plantillas no deben introducir decisiones de negocio, arquitectura, dominio o tecnología que todavía no hayan sido tomadas.

---

# 3. Plantilla de Requirement

Los Requirements definen qué necesita lograr el sistema o qué restricción debe satisfacer.

```md
# REQ-XXX-NNN — <Título del requisito>

## 1. Requisito

<Descripción clara del requisito.>

## 2. Tipo

- Funcional
- No funcional

## 3. Objetivo

<Por qué existe este requisito.>

## 4. Alcance

### Incluye

- <Elemento>

### No incluye

- <Elemento>

## 5. Criterios de aceptación

- <Criterio>

## 6. Restricciones

- <Restricción>

## 7. Dependencias

- <Dependencia>

## 8. Trazabilidad

- Especificaciones relacionadas:
  - <SPEC-ID>
  
Reglas del Requirement

Un Requirement debe:

expresar una necesidad, comportamiento esperado o restricción;
ser verificable cuando corresponda;
evitar describir directamente la implementación;
poder relacionarse con una o más Specifications;
utilizar el identificador definido por conventions.md.
4. Plantilla de Specification

Una Specification es el artefacto principal para definir el comportamiento de una capacidad del sistema.

La estructura siguiente representa la forma estándar de materializar una SPEC.

No todas las secciones son obligatorias para todas las capacidades.

Las secciones opcionales solo deben existir cuando correspondan.

# SPEC-XXX-NNN — <Título de la especificación>


## 11. Casos límite


- <Caso>


## 12. Criterios de aceptación


- <Criterio>


## 13. Dependencias


- <Dependencia>


## 14. Restricciones


- <Restricción>


## 15. Seguridad


<Requisitos de seguridad aplicables a la capacidad.>


## 16. API Contract


<Incluir únicamente cuando la capacidad sea expuesta mediante una API.>


### Método


`<Método HTTP>`


### Endpoint


`<Endpoint>`


### Autenticación


<Requisitos de autenticación en la frontera de la API.>


### Autorización


<Requisitos de autorización en la frontera de la API.>


### Headers


| Header | Obligatorio | Descripción |
|---|---|---|
| <Header> | Sí/No | <Descripción> |


### Parámetros de ruta


| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| <Parámetro> | <Tipo> | Sí/No | <Descripción> |


### Parámetros de consulta


| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| <Parámetro> | <Tipo> | Sí/No | <Descripción> |


### Request


```json
{
  "<campo>": "<valor>"
}
Response
{
  "<campo>": "<valor>"
}
Códigos HTTP
Código	Significado	Condición
<Código>	<Significado>	<Condición>
Errores
Código	Error	Condición
<Código>	<Error>	<Condición>
Paginación

<Contrato de paginación cuando corresponda.>

Filtros
<Contrato de filtrado cuando corresponda.>
Ordenamiento
<Contrato de ordenamiento cuando corresponda.>
Restricciones del contrato
<Restricción>
17. Referencias de dominio

Referencias a los conceptos de dominio afectados por esta especificación.

Entidad: <Referencia de dominio>
Invariante: <Referencia de dominio>
Estado: <Referencia de dominio>
Transición: <Referencia de dominio>
18. Trazabilidad
Requisitos:
<REQ-ID>
Dominio:
<Referencia de dominio>
Pruebas:
<TEST-ID>
ADR:
<ADR-ID>


### Reglas de la Specification


1. Una Specification es el documento principal de una capacidad de negocio.
2. El Caso de Uso forma parte de la Specification cuando corresponde.
3. El Caso de Uso no requiere un archivo independiente ni un identificador propio.
4. El API Contract forma parte de la Specification cuando la capacidad se expone mediante una API.
5. El API Contract no requiere un archivo independiente ni un identificador propio.
6. El API Contract describe la frontera externa de la API, no su implementación interna.
7. Los conceptos de dominio no deben duplicarse innecesariamente dentro de la Specification.
8. La Specification puede referenciar artefactos de dominio definidos en la documentación de dominio.
9. La sección de Seguridad define los requisitos de seguridad de la capacidad.
10. La sección de API Contract describe cómo esos requisitos se manifiestan en la frontera externa de la API.
11. Los detalles internos de implementación no deben introducirse en una Specification para completar artificialmente la plantilla.
12. Una Specification no debe incorporar decisiones arquitectónicas que todavía no hayan sido tomadas.
13. Cuando una Specification no sea expuesta mediante API, la sección API Contract no debe agregarse.


---


# 5. Plantilla de Dominio


La documentación de dominio describe el modelo interno del dominio y su comportamiento.


Los artefactos de dominio deben mantenerse independientes de los detalles de transporte, API o infraestructura.


```md
# <Identificador de dominio> — <Nombre del concepto>


## 1. Propósito


<Por qué existe este concepto de dominio.>


## 2. Definición


<Definición precisa del concepto.>


## 3. Responsabilidades


- <Responsabilidad>


## 4. Atributos


| Atributo | Tipo | Descripción |
|---|---|---|
| <Atributo> | <Tipo> | <Descripción> |


## 5. Invariantes


- <Invariante>


## 6. Estados


- <Estado>


## 7. Transiciones


| Desde | Evento | Hacia | Condiciones |
|---|---|---|---|
| <Estado> | <Evento> | <Estado> | <Condición> |


## 8. Relaciones


- <Relación>


## 9. Specifications relacionadas


- <SPEC-ID>


## 10. ADR relacionados


- <ADR-ID>
Reglas del dominio

La documentación de dominio debe describir:

conceptos del dominio;
responsabilidades;
comportamiento;
invariantes;
estados;
transiciones;
relaciones entre conceptos.

No debe utilizarse para documentar:

endpoints HTTP;
controllers;
DTOs;
headers;
códigos HTTP;
detalles de persistencia;
frameworks;
detalles de infraestructura;

salvo que otro documento normativo establezca explícitamente que determinado concepto forma parte del dominio.

La documentación de dominio debe utilizar las convenciones de identificación definidas en conventions.md.

6. Plantilla de ADR

Los Architecture Decision Records documentan decisiones significativas y el razonamiento que llevó a adoptarlas.

El mecanismo, autoridad y ciclo de vida de los ADR se definen en:

.claude/architecture/adr.md

Los ADR concretos del proyecto deben almacenarse en la ubicación definida por:

.claude/documentation/structure.md

# ADR-NNN — <Título de la decisión>


## 1. Estado


<Propuesto | Aceptado | Rechazado | Supersedido | Deprecado>


## 2. Fecha


<YYYY-MM-DD>


## 3. Contexto


<Problema, situación o presión arquitectónica que requiere una decisión.>


## 4. Decisión


<Decisión adoptada.>


## 5. Alternativas consideradas


### Alternativa A


<Descripción>


**Ventajas**


- <Ventaja>


**Desventajas**


- <Desventaja>


### Alternativa B


<Descripción>


**Ventajas**


- <Ventaja>


**Desventajas**


- <Desventaja>


## 6. Consecuencias


### Positivas


- <Consecuencia>


### Negativas


- <Consecuencia>


### Riesgos


- <Riesgo>


## 7. Áreas afectadas


- <Módulo / componente / capacidad>


## 8. Documentación relacionada


- Requisitos:
  - <REQ-ID>


- Specifications:
  - <SPEC-ID>


- Dominio:
  - <Referencia de dominio>


- ADR relacionados:
  - <ADR-ID>

La plantilla anterior define únicamente la estructura documental. Las reglas para determinar cuándo una decisión requiere un ADR, quién puede tomarla y cómo debe gestionarse su ciclo de vida corresponden a .claude/architecture/adr.md.

7. Plantilla de Trazabilidad

La documentación de trazabilidad relaciona requisitos, specifications, dominio, implementación, pruebas y decisiones arquitectónicas.

El mecanismo de trazabilidad está definido en:

.claude/sdd/traceability.md

La trazabilidad debe mantenerse de acuerdo con las reglas establecidas en dicho documento.

# Trazabilidad


## 1. Propósito


<Alcance de la trazabilidad documentada.>


## 2. Requirement → Specification


| Requirement | Specification | Estado |
|---|---|---|
| <REQ-ID> | <SPEC-ID> | <Estado> |


## 3. Specification → Dominio


| Specification | Concepto de dominio | Relación |
|---|---|---|
| <SPEC-ID> | <Referencia de dominio> | <Relación> |


## 4. Specification → Pruebas


| Specification | Prueba | Cobertura |
|---|---|---|
| <SPEC-ID> | <TEST-ID> | <Cobertura> |


## 5. Specification → ADR


| Specification | ADR | Motivo |
|---|---|---|
| <SPEC-ID> | <ADR-ID> | <Motivo> |


## 6. API Contract


Los API Contracts se referencian mediante su Specification correspondiente y no poseen identificador independiente.


| Specification | API Contract | Estado |
|---|---|---|
| <SPEC-ID> | Presente / No aplica | <Estado> |
Reglas de trazabilidad
El API Contract no constituye un nuevo eslabón de la cadena principal de trazabilidad.
El API Contract es una relación opcional de una Specification.
El Caso de Uso es una relación opcional de una Specification.
Tanto el Caso de Uso como el API Contract se identifican mediante la Specification que los contiene.
La estructura y obligatoriedad de las relaciones de trazabilidad deben seguir las reglas definidas en .claude/sdd/traceability.md.
8. Uso de las plantillas

Las plantillas deben utilizarse como punto de partida para crear documentación nueva.

El proceso correcto es:

Determinar qué tipo de artefacto se necesita.
Consultar las reglas normativas correspondientes.
Seleccionar la plantilla correspondiente.
Completar únicamente la información conocida y decidida.
Establecer las referencias y relaciones de trazabilidad correspondientes.
Verificar que no se haya duplicado información existente.
Verificar que el documento respete las convenciones de identificación.
Verificar que el documento se encuentre en la ubicación definida para ese tipo de artefacto.
Validar el documento contra las reglas del SDD antes de considerarlo terminado.

Las plantillas no autorizan por sí mismas la creación de decisiones.

Si para completar una sección es necesario tomar una decisión de negocio, dominio, arquitectura, seguridad o tecnología que todavía no fue tomada, la decisión debe resolverse mediante el mecanismo correspondiente antes de completar la documentación.

9. Principio de fuente única de verdad

Cada concepto debe tener una única fuente de verdad.

Las plantillas deben evitar que la misma información sea mantenida en múltiples documentos.

Ejemplos:

Las reglas de cómo se construye una Specification pertenecen a .claude/sdd/specification.md.
La estructura física de la documentación pertenece a .claude/documentation/structure.md.
Las convenciones de identificación pertenecen a .claude/documentation/conventions.md.
La estructura material de los documentos pertenece a este archivo.
Las reglas para los ADR pertenecen a .claude/architecture/adr.md.
Las reglas de trazabilidad pertenecen a .claude/sdd/traceability.md.
Los requisitos reales del proyecto pertenecen a docs/.
Las decisiones arquitectónicas reales del proyecto pertenecen a los ADR de docs/.
Las Specifications reales del proyecto pertenecen a la ubicación definida por la estructura documental.
Los conceptos de dominio reales del proyecto pertenecen a la documentación de dominio.

Cuando dos documentos necesiten referirse al mismo concepto, debe utilizarse una referencia antes que duplicar su contenido.

10. Regla de evolución

Las plantillas pueden evolucionar cuando el modelo documental del proyecto cambie.

Una modificación de una plantilla no debe utilizarse para introducir silenciosamente una nueva regla metodológica.

Cuando una modificación de plantilla implique un cambio en:

el modelo SDD;
la autoridad de una decisión;
el ciclo de vida;
la trazabilidad;
la arquitectura;
el modelo de dominio;
la seguridad;
las convenciones documentales;

primero debe modificarse el documento normativo correspondiente y posteriormente actualizar la plantilla.

La plantilla debe permanecer alineada con las reglas normativas existentes.