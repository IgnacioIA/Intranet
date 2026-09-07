package com.IntraNet.Laucom.security.infrastructure.config;

import com.IntraNet.Laucom.security.domain.port.TokenPort;
import com.IntraNet.Laucom.security.infrastructure.web.JwtAuthenticationFilter;
import com.IntraNet.Laucom.security.infrastructure.web.ProblemDetailAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ADR-014: Spring Security confinado al borde. `/auth/login`, `/auth/refresh`, `/auth/logout` y
 * `/auth/logout/all` (SPEC-AUTH-001/002/003) son públicos (ninguno requiere verificar el Access
 * Token — ver Javadoc de {@code AuthenticationController}); todo lo demás exige autenticación
 * (mínimo privilegio). Desde la Fase 16, {@link JwtAuthenticationFilter} puebla el
 * {@code SecurityContext} a partir de un Access Token verificado, y
 * {@link ProblemDetailAuthenticationEntryPoint} responde 401 + RFC 7807 cuando no hay uno válido
 * (primer endpoint protegido real: {@code GET /auth/me}, SPEC-AUTH-005).
 *
 * <p><b>Deliberadamente NO implementado todavía:</b> derivación de {@code GrantedAuthority} a
 * partir de los {@code Permission} vigentes (ADR-014) — ningún endpoint hasta esta fase exige un
 * permiso concreto (`GET /auth/me` solo exige estar autenticado); se agrega cuando la Fase 17
 * (endpoints administrativos) lo requiera de verdad.</p>
 *
 * <p>CSRF deshabilitado: la API es stateless (JWT + Refresh Token opaco, sin sesión de servidor
 * — ADR-007), y la mitigación de CSRF sobre {@code /auth/refresh} ya elegida por ADR-010 es
 * {@code SameSite=Strict} + {@code Path} restringido de la cookie, no el mecanismo de token
 * sincronizador de Spring Security (pensado para autenticación basada en sesión con formularios
 * server-side, que este módulo no usa).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityFilterChainConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenPort tokenPort) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout", "/auth/logout/all")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(new ProblemDetailAuthenticationEntryPoint()))
                .addFilterBefore(new JwtAuthenticationFilter(tokenPort), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
