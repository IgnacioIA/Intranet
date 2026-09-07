package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.identity.CurrentUserView;
import com.IntraNet.Laucom.security.application.identity.GetCurrentUserUseCase;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SPEC-AUTH-005 §12. `standaloneSetup`: la verificación real del Access Token (Spring Security
 * + JwtAuthenticationFilter) se cubre en JwtAuthenticationFilterTest; aquí solo se fija el
 * SecurityContext manualmente, como lo dejaría ese filtro tras verificar el token. */
@ExtendWith(MockitoExtension.class)
class CurrentUserControllerTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CurrentUserController controller = new CurrentUserController(getCurrentUser);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AuthenticationExceptionHandler())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
    }

    @Test
    void authenticatedUser_receivesTheirIdentityAndAuthorization() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticateAs(userId);
        CurrentUserView view = new CurrentUserView(userId, IdentityProvider.LOCAL, "jdoe", "Jane Doe",
                "jdoe@example.com", UserStatus.ACTIVE, Set.of("CONTENT_EDITOR"), Set.of("CONTENT_READ"));
        when(getCurrentUser.handle(userId)).thenReturn(Optional.of(view));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("CONTENT_EDITOR"))
                .andExpect(jsonPath("$.permissions[0]").value("CONTENT_READ"));
    }

    @Test
    void noAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.containsString("invalid-access-token")));
    }

    @Test
    void authenticatedButUserVanished_returns401() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticateAs(userId);
        when(getCurrentUser.handle(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.containsString("invalid-access-token")));
    }

    @Test
    void nullEmail_isSerializedAsJsonNull_notOmitted() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticateAs(userId);
        CurrentUserView view = new CurrentUserView(userId, IdentityProvider.ACTIVE_DIRECTORY, "jdoe", "Jane Doe",
                null, UserStatus.ACTIVE, Set.of(), Set.of());
        when(getCurrentUser.handle(userId)).thenReturn(Optional.of(view));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                // SPEC-AUTH-005 §12: "email se incluye y puede ser null" — presente, no omitido.
                // Se verifica por contenido crudo: jsonPath no distingue de forma confiable
                // "clave ausente" de "clave presente con valor null" (ambigüedad conocida de la
                // librería JsonPath subyacente a Spring MockMvc).
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("\"email\":null")));
    }
}
