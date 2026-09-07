package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.authentication.AuthenticateActiveDirectoryUserCommand;
import com.IntraNet.Laucom.security.application.authentication.AuthenticateActiveDirectoryUserUseCase;
import com.IntraNet.Laucom.security.application.authentication.AuthenticateLocalUserCommand;
import com.IntraNet.Laucom.security.application.authentication.AuthenticateLocalUserUseCase;
import com.IntraNet.Laucom.security.application.exception.InvalidRefreshTokenException;
import com.IntraNet.Laucom.security.application.session.IssuedSession;
import com.IntraNet.Laucom.security.application.session.LogoutAllCommand;
import com.IntraNet.Laucom.security.application.session.LogoutAllUseCase;
import com.IntraNet.Laucom.security.application.session.LogoutCommand;
import com.IntraNet.Laucom.security.application.session.LogoutUseCase;
import com.IntraNet.Laucom.security.application.session.RenewSessionCommand;
import com.IntraNet.Laucom.security.application.session.RenewSessionUseCase;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.LoginRequest;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.LoginResponse;
import com.IntraNet.Laucom.security.infrastructure.rest.dto.StatusResponse;
import com.IntraNet.Laucom.security.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * SPEC-AUTH-001 §12 (`POST /auth/login`), SPEC-AUTH-002 §12 (`POST /auth/refresh`) y
 * SPEC-AUTH-003 §12 (`POST /auth/logout`, `POST /auth/logout/all`). Transporte de tokens según
 * ADR-010: Access Token en el cuerpo, Refresh Token en cookie {@code HttpOnly}.
 *
 * <p><b>Limitaciones conocidas, señaladas explícitamente:</b></p>
 * <ul>
 *   <li>El {@code correlationId} se lee del atributo de request que fija
 *   {@link CorrelationIdFilter} (Fase 15, REQ-AUTH-024); si ese filtro no llegó a ejecutarse
 *   (ej. un test que invoca el controlador sin la cadena de filtros real) se genera uno de
 *   respaldo aquí mismo, para no fallar — pero en producción siempre proviene del filtro.</li>
 *   <li>La IP del cliente se toma de {@code HttpServletRequest#getRemoteAddr()}, que en un
 *   despliegue detrás de un reverse proxy devuelve la IP del proxy, no la del cliente real. Un
 *   despliegue con proxy requeriría un {@code ForwardedHeaderFilter} (provisto por Spring, no
 *   configurado aquí) — fuera del alcance de este módulo por ser una decisión de despliegue.</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    // Fase 22 (Security Review): corregido de "/auth/refresh" a "/auth". Un cookie Path restringe
    // a qué requests el NAVEGADOR adjunta el cookie (RFC 6265 §5.1.4) — con "/auth/refresh" el
    // navegador nunca lo habría enviado a POST /auth/logout ni /auth/logout/all (paths que no
    // empiezan con "/auth/refresh"), por lo que ambos endpoints siempre habrían recibido
    // refreshTokenCookie=null en un cliente real y jamás habrían revocado nada en el servidor
    // (el logout "funcionaba" solo porque MockMvc no simula el scoping por Path de un navegador
    // real). "/auth" cubre los cuatro endpoints que lo necesitan (login lo escribe; refresh,
    // logout y logout/all lo leen) sin exponerlo fuera de este controlador.
    private static final String REFRESH_COOKIE_PATH = "/auth";

    private final AuthenticateLocalUserUseCase authenticateLocalUser;
    private final AuthenticateActiveDirectoryUserUseCase authenticateActiveDirectoryUser;
    private final RenewSessionUseCase renewSession;
    private final LogoutUseCase logout;
    private final LogoutAllUseCase logoutAll;
    private final Clock clock;
    private final long accessTokenTtlSeconds;

    public AuthenticationController(AuthenticateLocalUserUseCase authenticateLocalUser,
                                     AuthenticateActiveDirectoryUserUseCase authenticateActiveDirectoryUser,
                                     RenewSessionUseCase renewSession, LogoutUseCase logout,
                                     LogoutAllUseCase logoutAll, Clock clock,
                                     @Value("${jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.authenticateLocalUser = authenticateLocalUser;
        this.authenticateActiveDirectoryUser = authenticateActiveDirectoryUser;
        this.renewSession = renewSession;
        this.logout = logout;
        this.logoutAll = logoutAll;
        this.clock = clock;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        IdentityProvider provider;
        try {
            provider = IdentityProvider.valueOf(request.provider());
        } catch (IllegalArgumentException e) {
            throw new InvalidProviderException(); // RN-08 SPEC-AUTH-001, ADR-018.
        }

        String clientIp = clientIp(httpRequest);
        String correlationId = correlationId(httpRequest);
        char[] password = request.password().toCharArray();

        IssuedSession session = switch (provider) {
            case LOCAL -> authenticateLocalUser.handle(
                    new AuthenticateLocalUserCommand(request.username(), password, clientIp), correlationId).session();
            case ACTIVE_DIRECTORY -> authenticateActiveDirectoryUser.handle(
                    new AuthenticateActiveDirectoryUserCommand(request.username(), password, clientIp),
                    correlationId).session();
        };

        setRefreshCookie(httpResponse, session);
        return ResponseEntity.ok(LoginResponse.of(session.accessToken(), accessTokenTtlSeconds));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        IssuedSession session = renewSession.handle(
                new RenewSessionCommand(refreshTokenCookie), correlationId(httpRequest));

        setRefreshCookie(httpResponse, session);
        return ResponseEntity.ok(LoginResponse.of(session.accessToken(), accessTokenTtlSeconds));
    }

    @PostMapping("/logout")
    public ResponseEntity<StatusResponse> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logout.handle(new LogoutCommand(refreshTokenCookie), correlationId(httpRequest)); // idempotente (UC-AUTH-006).
        clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @PostMapping("/logout/all")
    public ResponseEntity<StatusResponse> logoutAll(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logoutAll.handle(new LogoutAllCommand(refreshTokenCookie), correlationId(httpRequest)); // idempotente (UC-AUTH-007).
        clearRefreshCookie(httpResponse); // incluye la sesión actual, que también quedó revocada.
        return ResponseEntity.ok(StatusResponse.ok());
    }

    private void setRefreshCookie(HttpServletResponse response, IssuedSession session) {
        Duration maxAge = Duration.between(clock.instant(), session.refreshTokenExpiresAt());
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, session.refreshTokenSecret())
                .httpOnly(true)
                .secure(true)
                // "Apropiado para mismo sitio" (ADR-010): Strict es válido porque frontend y
                // backend están bajo el mismo sitio y el refresh nunca se dispara desde una
                // navegación de nivel superior entre sitios — decisión de bajo impacto y
                // reversible (Strict/Lax), no requiere ADR propio.
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** UC-AUTH-006 paso 4: instruye al cliente a eliminar la cookie de Refresh Token. */
    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /** REQ-AUTH-024: fijado por {@link CorrelationIdFilter}; UUID de respaldo si, por algún
     * motivo, ese filtro no se ejecutó (ver Javadoc de la clase). */
    private String correlationId(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        return attribute instanceof String value ? value : UUID.randomUUID().toString();
    }
}
