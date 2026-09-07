package com.IntraNet.Laucom.security.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Se ejecuta fuera del despacho normal de Spring MVC (a nivel de {@code Filter} de Spring
 * Security), por lo que no puede apoyarse en {@code @RestControllerAdvice}
 * ({@link com.IntraNet.Laucom.security.infrastructure.rest.AuthenticationExceptionHandler}) para
 * construir la respuesta: escribe el mismo formato de error (RFC 7807, {@code type} +
 * {@code invalid-access-token}) directamente sobre el {@code HttpServletResponse}.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final String ERROR_TYPE = "https://laucom.internal/errors/invalid-access-token";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", ERROR_TYPE);
        problem.put("title", "Access Token inválido");
        problem.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        problem.put("detail", "Access Token ausente, expirado o inválido");
        // Fase 19: mismo campo de extensión correlationId (REQ-AUTH-024) que el resto del
        // modelo de error del módulo (AuthenticationExceptionHandler).
        problem.put("correlationId", request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME));

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(problem));
    }
}
