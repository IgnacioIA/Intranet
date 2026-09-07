package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.exception.InvalidAccessTokenException;
import com.IntraNet.Laucom.security.application.identity.CurrentUserView;
import com.IntraNet.Laucom.security.application.identity.GetCurrentUserUseCase;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.CurrentUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * SPEC-AUTH-005 §12: {@code GET /auth/me}. Primer endpoint protegido real del módulo — requiere
 * Access Token vigente (ver {@code SecurityFilterChainConfig}, {@code JwtAuthenticationFilter}),
 * sin ningún permiso concreto adicional.
 */
@RestController
@RequestMapping("/auth")
public class CurrentUserController {

    private final GetCurrentUserUseCase getCurrentUser;

    public CurrentUserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        UUID userId = AuthenticatedActor.resolve();
        CurrentUserView view = getCurrentUser.handle(userId).orElseThrow(InvalidAccessTokenException::new);
        return CurrentUserResponse.from(view);
    }
}
