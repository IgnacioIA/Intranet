package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.exception.InvalidAccessTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Extrae el {@code userId} fijado por {@code JwtAuthenticationFilter} (Fase 16). Compartido por
 * todos los controladores que necesitan identificar al actor autenticado (Fase 17 en adelante),
 * en vez de repetir esta misma lectura del {@code SecurityContext} en cada uno.
 */
final class AuthenticatedActor {

    private AuthenticatedActor() {
    }

    static UUID resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new InvalidAccessTokenException();
        }
        return userId;
    }
}
