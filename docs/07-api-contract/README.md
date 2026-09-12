# API Contract generado — cómo mantenerlo sincronizado

## 1. Por qué existe este archivo

Cada Specification (`docs/06-specifications/SPEC-AUTH-*.md §12`) sigue siendo la fuente de verdad
sobre **por qué** existe cada endpoint y qué reglas de negocio hay detrás de cada respuesta. Este
directorio resuelve un problema distinto: darle al frontend (o a cualquier consumidor externo) un
contrato **máquina-legible e importable** (OpenAPI 3), sin mantenerlo a mano — para que nunca
pueda desincronizarse silenciosamente del código real.

## 2. Cómo se genera (siempre desde el código, nunca a mano)

El proyecto usa [springdoc-openapi](https://springdoc.org/) (`pom.xml`, dependencia
`springdoc-openapi-starter-webmvc-ui`). Introspecciona los `@RestController` reales (rutas,
DTOs, validaciones `@Valid`/`@NotBlank`, y las anotaciones `@Operation`/`@ApiResponse`/`@Tag`
agregadas a cada método) y genera el documento OpenAPI **en cada arranque de la aplicación** —
nunca es un archivo redactado a mano que alguien tenga que acordarse de actualizar.

Con la aplicación corriendo (`mvn spring-boot:run`, o el jar empaquetado), el contrato está
disponible en:

- `GET /v3/api-docs` — JSON.
- `GET /v3/api-docs.yaml` — YAML.
- `GET /swagger-ui.html` — UI interactiva (explorar y probar cada endpoint, incluido el botón
  "Authorize" para pegar un Access Token Bearer).

**Esta es la fuente de verdad viva.** Si cambia un `@RestController`, un DTO, o una anotación
`@Operation`, el resultado de estos tres endpoints cambia automáticamente en el siguiente
arranque — no requiere ningún paso manual adicional.

## 3. Archivo estático versionado (`openapi.json` de este directorio)

Para un frontend que quiera importar el contrato en Postman/Insomnia, generar un cliente
TypeScript, o simplemente revisar un diff en una PR sin levantar la aplicación completa, este
directorio contiene una copia exportada: `openapi.json`.

Esa copia **se regenera con un solo comando**, nunca se edita a mano:

```bash
mvn verify -Pgenerate-openapi
```

### Qué hace ese comando
1. Arranca la aplicación empaquetada (`spring-boot-maven-plugin`, goals `start`/`stop`, fase
   `pre-integration-test`/`post-integration-test`).
2. `springdoc-openapi-maven-plugin` (fase `integration-test`) pide `GET /v3/api-docs` a esa
   instancia recién arrancada y vuelca la respuesta a `docs/07-api-contract/openapi.json`.
3. Apaga la aplicación.

### Prerrequisitos para poder correrlo
Arranca la aplicación **de verdad** (no un contexto de test simulado), así que necesita lo mismo
que un despliegue real (ver `application.properties`):
- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` — MySQL accesible.
- `JWT_PRIVATE_KEY_BASE64` / `JWT_PUBLIC_KEY_BASE64` — claves reales (sin esto, la app no arranca).
- Si todavía no existe ningún administrador local: `MASTER_ADMIN_BOOTSTRAP_PASSWORD_HASH` válido
  (si falta, la app falla al arrancar por diseño — RN-07 SPEC-AUTH-009, fail-fast).

**Por eso este comando no es parte del build normal** (`mvn test`/`mvn package`/`mvn verify` sin
`-P` no lo ejecutan): no todo entorno tiene un MySQL a mano (ej. una máquina de desarrollo sin
Docker). Correrlo en CI, contra una base de datos de esa misma pipeline, es el lugar natural para
mantener este archivo actualizado automáticamente en cada cambio relevante.

## 4. Qué hacer al agregar/cambiar un endpoint

1. Escribí el `@RestController` (o modificalo) como ya se hace en el resto del módulo.
2. Agregale `@Operation`/`@ApiResponse` (ver cualquier controlador existente en
   `infrastructure.rest` como ejemplo) — esto es lo que le da al contrato generado las
   descripciones humanas, no solo la forma técnica.
3. Si el endpoint necesita una regla de negocio nueva, documentala primero en la Specification
   correspondiente (`docs/06-specifications/`) — igual que siempre, el código no inventa reglas
   que la SPEC no respalda.
4. No hace falta tocar `openapi.json` manualmente. Si querés refrescarlo para una PR, corré
   `mvn verify -Pgenerate-openapi` (con una base de datos disponible) y commiteá el resultado.

## 5. Relación con las Specifications

| Necesitás... | Consultá... |
|---|---|
| Por qué existe un endpoint, sus reglas de negocio, casos límite | `docs/06-specifications/SPEC-AUTH-*.md §12` |
| Forma exacta de cada request/response, para escribir código de cliente | `/swagger-ui.html` (en vivo) u `openapi.json` (estático, este directorio) |
| Todos los endpoints juntos, para explorar/probar sin leer 10 archivos | `/swagger-ui.html` |
