# ADR-003 — Representación local (Shadow Identity) de usuarios de Active Directory

## 1. Estado
Propuesto

## 2. Fecha
2026-09-04

## 3. Contexto
La aplicación necesita asociar a cada usuario de Active Directory un estado, roles y permisos propios, sin duplicar la autoridad de AD sobre la autenticación ni copiar innecesariamente sus atributos.

## 4. Decisión
Se crea una representación local (`User` con `provider = ACTIVE_DIRECTORY`) por cada usuario AD que se autentica al menos una vez. Esta representación almacena únicamente lo necesario para la aplicación (identidad estable, username, nombre visible, email, estado, roles) y nunca la contraseña ni atributos de AD no utilizados por la aplicación. La representación se conserva aunque el usuario desaparezca o sea deshabilitado en AD, para preservar historial y auditoría.

## 5. Alternativas consideradas

### Alternativa A — No persistir representación local; resolver roles en cada request contra AD
**Ventajas:** sin duplicación de datos.
**Desventajas:** acopla cada request a la disponibilidad de AD; imposibilita distinguir roles explícitos de roles derivados; imposibilita conservar historial si el usuario desaparece de AD.

### Alternativa B — Shadow Identity persistida (elegida)
**Ventajas:** permite autorización viva contra DB sin dependencia de AD en cada request; permite el modelo de doble procedencia de roles; preserva historial.
**Desventajas:** requiere sincronización explícita y bien definida (ver SPEC-AUTH-001).

## 6. Consecuencias

### Positivas
- Autorización desacoplada de la disponibilidad de AD tras el login.
- Historial y auditoría preservados aunque el usuario deje de existir en AD.

### Negativas
- Introduce la necesidad de una lógica de sincronización explícita (recalcular roles derivados en cada login).

### Riesgos
- Divergencia entre el estado de AD y la Shadow Identity si no se sincroniza correctamente. Mitigación: recalcular roles derivados en cada login exitoso (ver ADR-005, REQ-AUTH-007).

## 7. Áreas afectadas
Domain (`User`), Application (provisión y sincronización).

## 8. Documentación relacionada
- Requisitos: REQ-AUTH-002, REQ-AUTH-005, REQ-AUTH-007
- Specifications: SPEC-AUTH-001
- Dominio: User
- ADR relacionados: ADR-004, ADR-005
