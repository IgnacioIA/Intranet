package com.IntraNet.Laucom.security.infrastructure.web;

import com.IntraNet.Laucom.security.domain.model.AccessTokenClaims;
import com.IntraNet.Laucom.security.domain.port.TokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * ADR-014: integración de Spring Security confinada al borde. Verifica el Access Token
 * ({@code TokenPort#parse}, Fase 8) y, si es válido, puebla el {@code SecurityContext} con el
 * {@code userId} como principal — nada más. No deriva {@code GrantedAuthority} todavía: ningún
 * endpoint hasta esta fase exige un permiso concreto (`GET /auth/me` solo exige "estar
 * autenticado", SPEC-AUTH-005 §12); la derivación de autoridades se agrega cuando la Fase 17
 * la necesite de verdad (endpoints administrativos con `@PreAuthorize`/permiso concreto).
 *
 * <p>Un token ausente o inválido simplemente no puebla el contexto — no rechaza aquí mismo: es
 * {@code authorizeHttpRequests().anyRequest().authenticated()} (Fase 10) quien deniega, y
 * {@link ProblemDetailAuthenticationEntryPoint} quien construye la respuesta 401.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenPort tokenPort;

    public JwtAuthenticationFilter(TokenPort tokenPort) {
        this.tokenPort = tokenPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        extractBearerToken(request)
                .flatMap(tokenPort::parse)
                .ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private void authenticate(AccessTokenClaims claims) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(claims.userId(), null, List.of());
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
