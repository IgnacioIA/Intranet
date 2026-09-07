package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.authentication.AuthenticateActiveDirectoryUserUseCase;
import com.IntraNet.Laucom.security.application.authentication.AuthenticateLocalUserUseCase;
import com.IntraNet.Laucom.security.application.authentication.AuthenticationResult;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.exception.RefreshTokenReuseDetectedException;
import com.IntraNet.Laucom.security.application.session.IssuedSession;
import com.IntraNet.Laucom.security.application.session.LogoutAllUseCase;
import com.IntraNet.Laucom.security.application.session.LogoutUseCase;
import com.IntraNet.Laucom.security.application.session.RenewSessionUseCase;
import com.IntraNet.Laucom.security.domain.model.AccessToken;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC-AUTH-001 §12 / SPEC-AUTH-002 §12, ADR-010. `standaloneSetup`: sin contexto de Spring, con
 * los Use Cases mockeados — evita la fragilidad de un slice `@WebMvcTest` completo para esta
 * verificación (que solo necesita comprobar mapeo HTTP, no auto-configuración de Spring Boot).
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private AuthenticateLocalUserUseCase authenticateLocalUser;
    @Mock
    private AuthenticateActiveDirectoryUserUseCase authenticateActiveDirectoryUser;
    @Mock
    private RenewSessionUseCase renewSession;
    @Mock
    private LogoutUseCase logoutUseCase;
    @Mock
    private LogoutAllUseCase logoutAllUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuthenticationController controller = new AuthenticationController(authenticateLocalUser,
                authenticateActiveDirectoryUser, renewSession, logoutUseCase, logoutAllUseCase, clock, 900);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AuthenticationExceptionHandler())
                // Fase 15: se registra el filtro real (no solo se asume que funciona) para que
                // estos tests ejerzan el mismo camino de correlationId que producción.
                .addFilter(new com.IntraNet.Laucom.security.infrastructure.web.CorrelationIdFilter())
                .build();
    }

    private static IssuedSession aSession() {
        return new IssuedSession(new AccessToken("jwt-value", NOW.plusSeconds(900)),
                "refresh-secret-value", NOW.plusSeconds(604800));
    }

    private static User aUser() {
        User user = User.createLocal(UUID.randomUUID(), "jdoe", "Jane Doe", "jdoe@example.com",
                PasswordCredential.of("hash", false), NOW);
        user.assignRole(UserRoleAssignment.grantedExplicitly(UUID.randomUUID(), NOW), NOW);
        return user;
    }

    @Test
    void localLogin_returnsAccessToken_andSetsHttpOnlyRefreshCookie() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenReturn(new AuthenticationResult(aUser(), aSession()));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/auth"))
                .andExpect(cookie().value("refreshToken", "refresh-secret-value"));
    }

    @Test
    void everyResponse_carriesACorrelationIdHeader_REQ_AUTH_024() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenReturn(new AuthenticationResult(aUser(), aSession()));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("X-Correlation-Id"));

        org.mockito.ArgumentCaptor<String> correlationIdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(authenticateLocalUser).handle(any(), correlationIdCaptor.capture());
        assertThat(correlationIdCaptor.getValue()).isNotBlank();
    }

    @Test
    void aClientSuppliedCorrelationId_isReusedInTheResponseAndPropagatedToTheUseCase() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenReturn(new AuthenticationResult(aUser(), aSession()));

        mockMvc.perform(post("/auth/login")
                        .header("X-Correlation-Id", "client-trace-42")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Correlation-Id", "client-trace-42"));

        org.mockito.Mockito.verify(authenticateLocalUser).handle(any(), org.mockito.ArgumentMatchers.eq("client-trace-42"));
    }

    @Test
    void activeDirectoryLogin_dispatchesToTheAdUseCase() throws Exception {
        when(authenticateActiveDirectoryUser.handle(any(), any()))
                .thenReturn(new AuthenticationResult(aUser(), aSession()));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"ACTIVE_DIRECTORY\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidProvider_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"BOGUS\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("invalid-provider")));
    }

    @Test
    void invalidCredentials_returns401_withGenericProblemBody() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenThrow(new InvalidCredentialException());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(containsString("invalid-credentials")))
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void rateLimitExceeded_returns429() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenThrow(new RateLimitExceededException());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value(containsString("rate-limit-exceeded")));
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(containsString("invalid-refresh-token")));
    }

    @Test
    void refresh_withValidCookie_rotatesAndSetsNewCookie() throws Exception {
        when(renewSession.handle(any(), any())).thenReturn(aSession());

        mockMvc.perform(post("/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(cookie().value("refreshToken", "refresh-secret-value"))
                // Fase 22: el Path debe seguir cubriendo /auth/logout y /auth/logout/all, no solo
                // /auth/refresh — de lo contrario un navegador real nunca reenviaría el cookie a
                // esos endpoints (RFC 6265 §5.1.4).
                .andExpect(cookie().path("refreshToken", "/auth"));
    }

    @Test
    void refresh_reuseDetected_returns401_withDistinctType_butSameGenericDetailAsInvalidToken() throws Exception {
        when(renewSession.handle(any(), any())).thenThrow(new RefreshTokenReuseDetectedException());

        mockMvc.perform(post("/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "reused-secret")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(containsString("refresh-token-reused")))
                .andExpect(jsonPath("$.type").value(not(containsString("invalid-refresh-token"))));
    }

    @Test
    void logout_withCookie_revokesAndClearsTheCookie() throws Exception {
        mockMvc.perform(post("/auth/logout").cookie(new jakarta.servlet.http.Cookie("refreshToken", "some-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().value("refreshToken", ""))
                // Fase 22: la instrucción de borrado debe usar el mismo Path con el que se fijó el
                // cookie originalmente — si difieren, el navegador los trata como dos cookies
                // distintas y el original nunca se borra (RFC 6265 §5.3).
                .andExpect(cookie().path("refreshToken", "/auth"));

        org.mockito.Mockito.verify(logoutUseCase).handle(org.mockito.ArgumentMatchers.eq(
                new com.IntraNet.Laucom.security.application.session.LogoutCommand("some-secret")),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void logout_withoutCookie_isStillSuccessful_idempotent() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void logoutAll_withCookie_revokesAndClearsTheCookie() throws Exception {
        mockMvc.perform(post("/auth/logout/all").cookie(new jakarta.servlet.http.Cookie("refreshToken", "some-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().path("refreshToken", "/auth")); // Fase 22: ver logout_withCookie_....

        org.mockito.Mockito.verify(logoutAllUseCase).handle(org.mockito.ArgumentMatchers.eq(
                new com.IntraNet.Laucom.security.application.session.LogoutAllCommand("some-secret")),
                org.mockito.ArgumentMatchers.any());
    }

    // --- Fase 19 (API Error Model): huecos genéricos del mecanismo de manejo de errores ---

    @Test
    void beanValidationFailure_returns400_withFieldDetails() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("invalid-request")))
                .andExpect(jsonPath("$.detail").value(containsString("password")));
    }

    @Test
    void malformedJsonBody_returns400_withoutLeakingTheRawBody() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(containsString("malformed-request-body")))
                .andExpect(jsonPath("$.detail").value(not(containsString("not-valid-json"))));
    }

    @Test
    void unexpectedException_returns500_withAGenericDetail_neverTheRealMessage() throws Exception {
        when(authenticateLocalUser.handle(any(), any()))
                .thenThrow(new IllegalStateException("some internal detail that must never reach the client"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"secret123456\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value(containsString("internal-error")))
                .andExpect(jsonPath("$.detail").value(not(containsString("internal detail"))));
    }

    @Test
    void everyErrorResponse_includesTheCorrelationId() throws Exception {
        when(authenticateLocalUser.handle(any(), any())).thenThrow(new InvalidCredentialException());

        mockMvc.perform(post("/auth/login")
                        .header("X-Correlation-Id", "trace-for-error-42")
                        .contentType("application/json")
                        .content("{\"provider\":\"LOCAL\",\"username\":\"jdoe\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("trace-for-error-42"));
    }
}
