package com.IntraNet.Laucom.security.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata global de OpenAPI (springdoc-openapi), generada en vivo a partir de las anotaciones
 * de cada controlador — nunca un archivo mantenido a mano, para que no pueda desincronizarse del
 * código (ver `docs/07-api-contract/README.md`). Expuesta en {@code /v3/api-docs} (JSON),
 * {@code /v3/api-docs.yaml} y una UI interactiva en {@code /swagger-ui.html}.
 *
 * <p>El esquema de seguridad declarado ({@code bearerAuth}) cubre el Access Token
 * (ADR-010: se envía en el header {@code Authorization: Bearer <token>}). El Refresh Token
 * (cookie {@code HttpOnly}, {@code Path=/auth}) no se modela como esquema de seguridad de
 * OpenAPI porque Swagger UI no puede "probarlo" de forma útil vía cookie httpOnly — cada
 * endpoint que lo consume lo documenta igualmente en su descripción (ver
 * {@code AuthenticationController}).</p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Laucom — Security/Auth API",
                version = "1.0",
                description = "Autenticación, sesiones, autorización (RBAC) y administración de "
                        + "identidad del módulo Security/Auth. Ver las Specifications en "
                        + "docs/06-specifications/ para las reglas de negocio detrás de cada "
                        + "endpoint (códigos de error, invariantes, casos límite)."),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Access Token emitido por POST /auth/login o POST /auth/refresh (ADR-010). "
                + "No requerido por /auth/login, /auth/refresh, /auth/logout ni /auth/logout/all."
)
public class OpenApiConfig {
}
