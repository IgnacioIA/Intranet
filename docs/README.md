# Documentación de Laucom

Punto de entrada a la documentación específica del proyecto. Las reglas metodológicas (cómo se trabaja, no qué se construye) viven en `.claude/`; este árbol contiene el conocimiento durable de Laucom.

## Cómo navegar

| Carpeta/archivo | Contenido |
|---|---|
| [`PROJECT.md`](PROJECT.md) | Contexto general del proyecto. *(Pendiente — fuera del alcance del Módulo de Seguridad)* |
| [`GLOSSARY.md`](GLOSSARY.md) | Vocabulario oficial y contextos de identificadores registrados. |
| [`01-requirements/`](01-requirements/) | Requisitos funcionales y no funcionales (`REQ-<CONTEXTO>-NNN`). |
| [`02-domain/`](02-domain/) | Modelo de dominio: entidades, invariantes, estados, transiciones. |
| [`03-architecture/`](03-architecture/) | Arquitectura, infraestructura y seguridad adoptadas por Laucom. |
| [`04-adr/`](04-adr/) | Architecture Decision Records (`ADR-NNN`). |
| [`05-traceability/`](05-traceability/) | Matriz de trazabilidad REQ → SPEC → DOMAIN → ADR → TEST. |
| [`06-specifications/`](06-specifications/) | Especificaciones funcionales (`SPEC-<CONTEXTO>-NNN`), con sus casos de uso y API Contracts embebidos. |

## Antes de trabajar sobre el proyecto

1. Leer `.claude/CLAUDE.md` y las reglas especializadas relevantes (`.claude/core/`, `.claude/sdd/`).
2. Consultar `GLOSSARY.md` para confirmar contextos y vocabulario vigente.
3. Revisar los Requirements y Specifications del área afectada antes de proponer cambios.
4. Verificar `04-adr/` para decisiones arquitectónicas ya vigentes que puedan condicionar el trabajo.

## Estado actual

Primer conjunto de documentación formal, correspondiente al **Módulo de Seguridad (contexto `AUTH`)**: identidad LOCAL/Active Directory, autenticación, autorización RBAC, tokens, auditoría de seguridad. Todas las Specifications se encuentran en estado `IN_REVIEW` (ver `.claude/sdd/lifecycle.md`) — listas para revisión humana, todavía no aprobadas.
