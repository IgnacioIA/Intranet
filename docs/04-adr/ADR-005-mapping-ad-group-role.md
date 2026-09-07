# ADR-005 — Mapping explícito AD Group → Application Role → Permissions, almacenado en base de datos

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
La pertenencia a un grupo de Active Directory no debe traducirse automáticamente en permisos de la aplicación, porque la estructura de grupos de AD está fuera del control de esta aplicación y puede cambiar por razones ajenas a ella. Es necesario decidir cómo y dónde se almacena la relación entre un grupo AD y un rol de la aplicación.

## 4. Decisión
Se define una entidad `AdGroupRoleMapping` persistida en base de datos, administrable mediante un permiso administrativo dedicado, con auditoría de cada alta/baja/modificación (autor y fecha). Un grupo AD sin entrada en esta tabla no otorga ningún rol.

## 5. Alternativas consideradas

### Alternativa A — Archivo de configuración (YAML/properties)
**Ventajas:** versionado en Git, simple de leer.
**Desventajas:** requiere redeploy para cambiar; no tiene auditoría de "quién cambió qué mapping y cuándo" en runtime.

### Alternativa B — Hardcodeado en código
**Ventajas:** ninguna relevante frente a las otras opciones.
**Desventajas:** requiere recompilar y redesplegar para cualquier cambio; inaceptable para un ajuste operativo frecuente.

### Alternativa C — Base de datos con auditoría (elegida)
**Ventajas:** modificable sin redeploy; auditable; consistente con el resto del modelo de autorización, que ya vive en base de datos.
**Desventajas:** requiere una pantalla/API administrativa mínima (SPEC-AUTH-008) en lugar de solo editar un archivo.

## 6. Consecuencias

### Positivas
- Cambios de mapping sin ventana de despliegue.
- Trazabilidad de quién autorizó cada mapping.

### Negativas
- Requiere construir la capacidad administrativa mínima (SPEC-AUTH-008) en V1 en lugar de diferirla.

### Riesgos
- Drift entre grupos reales de AD y mappings configurados si nadie revisa los eventos `AD_GROUP_UNMAPPED`. Mitigación: estos eventos quedan en Security Audit para revisión periódica (proceso operativo, fuera del alcance de este ADR).

## 7. Áreas afectadas
Domain (`AdGroupRoleMapping`, `Role`), Application (sincronización de roles), Persistence.

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-004, REQ-AUTH-005, REQ-AUTH-007
- Specifications: SPEC-AUTH-001, SPEC-AUTH-008
- Dominio: AdGroupRoleMapping (INV-AUTH-010), UserRoleAssignment (INV-AUTH-005)
- ADR relacionados: ADR-003
