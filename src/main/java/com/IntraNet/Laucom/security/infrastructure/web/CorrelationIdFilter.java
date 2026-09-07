package com.IntraNet.Laucom.security.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * REQ-AUTH-024: asigna un {@code correlationId} a cada solicitud, propagado a logs técnicos
 * (vía MDC) y disponible para los controladores (vía atributo de request), de forma que un
 * evento de auditoría y las líneas de log de la misma solicitud puedan correlacionarse.
 *
 * <p>Mecanismo elegido: {@code Filter} servlet dedicado (architecture.md §4) — concern
 * por-request, no por-método; más simple e idiomático que AOP para esto.</p>
 *
 * <p>Si el cliente ya trae {@code X-Correlation-Id} (ej. propagado desde un proxy o desde el
 * propio frontend para correlacionar con sus propios logs), se reutiliza — pero solo si tiene
 * una forma segura (validación de entrada): de lo contrario se genera uno nuevo, para no
 * persistir/loguear un valor arbitrario provisto por el cliente (longitud no acotada, caracteres
 * de control que podrían inyectar líneas falsas en el log).</p>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE_NAME = "correlationId";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        request.setAttribute(REQUEST_ATTRIBUTE_NAME, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // evita fuga entre requests reutilizando el mismo hilo (pool de Tomcat).
        }
    }

    private static String resolveCorrelationId(String incoming) {
        if (incoming != null && SAFE_CORRELATION_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
