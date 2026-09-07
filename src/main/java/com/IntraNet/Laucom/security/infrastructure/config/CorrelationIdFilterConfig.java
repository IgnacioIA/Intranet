package com.IntraNet.Laucom.security.infrastructure.config;

import com.IntraNet.Laucom.security.infrastructure.web.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra {@link CorrelationIdFilter} con la precedencia más alta posible: debe ejecutarse
 * antes que el {@code SecurityFilterChain} (Fase 10) para que incluso una respuesta 401/403 de
 * Spring Security ya lleve el header {@code X-Correlation-Id} y su correspondiente entrada MDC.
 */
@Configuration
public class CorrelationIdFilterConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
