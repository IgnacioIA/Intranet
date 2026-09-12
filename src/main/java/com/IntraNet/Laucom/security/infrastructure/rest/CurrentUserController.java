package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.exception.InvalidAccessTokenException;
import com.IntraNet.Laucom.security.application.identity.CurrentUserView;
import com.IntraNet.Laucom.security.application.identity.GetCurrentUserUseCase;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Identity", description = "SPEC-AUTH-005.")
public class CurrentUserController {

    private final GetCurrentUserUseCase getCurrentUser;

    public CurrentUserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @Operation(summary = "Identidad y autorización del usuario autenticado",
            description = "SPEC-AUTH-005 §12. `email` puede ser `null` (ej. Shadow Identity de AD sin "
                    + "atributo de correo mapeado). Un usuario `PENDING_ONBOARDING` recibe el rol/permiso "
                    + "virtuales de onboarding, no persistidos como `Role`/`Permission` reales.")
    @ApiResponse(responseCode = "200", description = "Identidad, roles y permisos vigentes.")
    @ApiResponse(responseCode = "401", description = "Access Token ausente, expirado o inválido (`invalid-access-token`).")
    @GetMapping("/me")
    public CurrentUserResponse me() {
        UUID userId = AuthenticatedActor.resolve();
        CurrentUserView view = getCurrentUser.handle(userId).orElseThrow(InvalidAccessTokenException::new);
        return CurrentUserResponse.from(view);
    }
}
