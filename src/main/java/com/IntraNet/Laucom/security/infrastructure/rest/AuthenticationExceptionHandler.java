package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.AdGroupMappingNotFoundException;
import com.IntraNet.Laucom.security.application.exception.AssignmentNotExplicitException;
import com.IntraNet.Laucom.security.application.exception.DirectoryGroupLookupFailedException;
import com.IntraNet.Laucom.security.application.exception.DirectoryUnavailableException;
import com.IntraNet.Laucom.security.application.exception.IdentityManagedExternallyException;
import com.IntraNet.Laucom.security.application.exception.InsufficientPermissionException;
import com.IntraNet.Laucom.security.application.exception.InvalidAccessTokenException;
import com.IntraNet.Laucom.security.application.exception.InvalidCredentialException;
import com.IntraNet.Laucom.security.application.exception.InvalidRefreshTokenException;
import com.IntraNet.Laucom.security.application.exception.MasterAdminContinuityViolationException;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.application.exception.PermissionAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.PermissionNameImmutableException;
import com.IntraNet.Laucom.security.application.exception.PermissionNotFoundException;
import com.IntraNet.Laucom.security.application.exception.RateLimitExceededException;
import com.IntraNet.Laucom.security.application.exception.RefreshTokenReuseDetectedException;
import com.IntraNet.Laucom.security.application.exception.RoleAlreadyExistsException;
import com.IntraNet.Laucom.security.application.exception.RoleInactiveException;
import com.IntraNet.Laucom.security.application.exception.RoleNotFoundException;
import com.IntraNet.Laucom.security.application.exception.UserNotFoundException;
import com.IntraNet.Laucom.security.application.exception.UsernameAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.exception.InvalidUserStateTransitionException;
import com.IntraNet.Laucom.security.domain.exception.SystemPermissionProtectedException;
import com.IntraNet.Laucom.security.domain.exception.SystemRoleProtectedException;
import com.IntraNet.Laucom.security.infrastructure.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Fase 19 (API Error Model): mapea las excepciones de aplicación/dominio de todo el módulo a su
 * código HTTP y tipo RFC 7807, y cierra los dos huecos que quedaban fuera de este mecanismo desde
 * que se introdujo en la Fase 10: errores de validación de Bean Validation
 * ({@code MethodArgumentNotValidException}), cuerpos JSON malformados
 * ({@code HttpMessageNotReadableException}) y cualquier excepción no anticipada (catch-all, sin
 * filtrar detalles internos al cliente — REQ-AUTH-023/principio de no exponer información
 * interna del sistema, ya aplicado puntualmente en SPEC-AUTH-007 RN-01 y generalizado aquí).
 *
 * <p>Todo {@link ProblemDetail} incluye, además de {@code type}/{@code title}/{@code status}/
 * {@code detail} estándar de RFC 7807, la propiedad de extensión {@code correlationId}
 * (REQ-AUTH-024): permite
 * que un cliente que recibe un error lo referencie exactamente en logs/auditoría al reportarlo.
 *
 * <p>Ámbito deliberadamente acotado a este paquete ({@code basePackages}), para no imponer un
 * mecanismo de manejo de errores al resto de la aplicación (fuera del alcance de este módulo).</p>
 */
@RestControllerAdvice(basePackages = "com.IntraNet.Laucom.security.infrastructure.rest")
class AuthenticationExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);
    private static final String ERROR_BASE_URI = "https://laucom.internal/errors/";
    private static final String REFRESH_GENERIC_DETAIL = "Refresh Token inválido o expirado";

    @ExceptionHandler(InvalidCredentialException.class)
    ProblemDetail handleInvalidCredential() {
        // RN-06 SPEC-AUTH-001: mensaje genérico único, sin distinguir la causa real.
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Credenciales inválidas");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ProblemDetail handleRateLimitExceeded() {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "rate-limit-exceeded", "Límite de intentos excedido");
    }

    @ExceptionHandler(DirectoryUnavailableException.class)
    ProblemDetail handleDirectoryUnavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "ad-unavailable",
                "Servicio de autenticación de directorio no disponible");
    }

    @ExceptionHandler(DirectoryGroupLookupFailedException.class)
    ProblemDetail handleGroupLookupFailed() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "ad-sync-failed",
                "No se pudieron evaluar los grupos de Active Directory del usuario");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken() {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", REFRESH_GENERIC_DETAIL);
    }

    @ExceptionHandler(RefreshTokenReuseDetectedException.class)
    ProblemDetail handleRefreshTokenReuseDetected() {
        // SPEC-AUTH-002 §12 "Restricciones del contrato": mismo detail que invalid-refresh-token
        // — la distinción es solo interna/de auditoría; únicamente el "type" difiere.
        return problem(HttpStatus.UNAUTHORIZED, "refresh-token-reused", REFRESH_GENERIC_DETAIL);
    }

    @ExceptionHandler(InvalidProviderException.class)
    ProblemDetail handleInvalidProvider() {
        return problem(HttpStatus.BAD_REQUEST, "invalid-provider", "provider debe ser LOCAL o ACTIVE_DIRECTORY");
    }

    @ExceptionHandler(InvalidAccessTokenException.class)
    ProblemDetail handleInvalidAccessToken() {
        // Mismo "type" que ProblemDetailAuthenticationEntryPoint (Fase 16): ambos cubren la
        // misma condición (SPEC-AUTH-005 §12), solo difiere en qué capa la detecta.
        return problem(HttpStatus.UNAUTHORIZED, "invalid-access-token", "Access Token ausente, expirado o inválido");
    }

    // --- SPEC-AUTH-010 (Fase 17) y de aplicación transversal (Fase 6, sin endpoint hasta ahora) ---

    @ExceptionHandler(InsufficientPermissionException.class)
    ProblemDetail handleInsufficientPermission() {
        return problem(HttpStatus.FORBIDDEN, "insufficient-permissions", "Permiso insuficiente para esta operación");
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound() {
        return problem(HttpStatus.NOT_FOUND, "user-not-found", "Usuario inexistente");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail handleUsernameAlreadyExists() {
        return problem(HttpStatus.CONFLICT, "username-already-exists", "Ya existe un usuario LOCAL con ese username");
    }

    @ExceptionHandler(IdentityManagedExternallyException.class)
    ProblemDetail handleIdentityManagedExternally() {
        return problem(HttpStatus.BAD_REQUEST, "identity-managed-externally",
                "Este atributo de la identidad es gestionado por Active Directory, no por la aplicación");
    }

    @ExceptionHandler(InvalidUserStateTransitionException.class)
    ProblemDetail handleInvalidUserStateTransition(InvalidUserStateTransitionException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-state-transition", e.getMessage());
    }

    @ExceptionHandler(MasterAdminContinuityViolationException.class)
    ProblemDetail handleMasterAdminContinuityViolation() {
        return problem(HttpStatus.CONFLICT, "master-admin-continuity-violation",
                "La operación dejaría al sistema sin ningún administrador MASTER_ADMIN activo");
    }

    @ExceptionHandler(RoleNotFoundException.class)
    ProblemDetail handleRoleNotFound() {
        return problem(HttpStatus.NOT_FOUND, "role-not-found", "Role inexistente");
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    ProblemDetail handleRoleAlreadyExists() {
        return problem(HttpStatus.CONFLICT, "role-already-exists", "Ya existe un Role con ese name");
    }

    @ExceptionHandler(SystemRoleProtectedException.class)
    ProblemDetail handleSystemRoleProtected(SystemRoleProtectedException e) {
        return problem(HttpStatus.CONFLICT, "system-role-protected", e.getMessage());
    }

    @ExceptionHandler(RoleInactiveException.class)
    ProblemDetail handleRoleInactive() {
        return problem(HttpStatus.BAD_REQUEST, "role-inactive", "No se puede asignar un Role inactivo");
    }

    @ExceptionHandler(AssignmentNotExplicitException.class)
    ProblemDetail handleAssignmentNotExplicit(AssignmentNotExplicitException e) {
        return problem(HttpStatus.CONFLICT, "assignment-not-explicit", e.getMessage());
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    ProblemDetail handlePermissionNotFound() {
        return problem(HttpStatus.NOT_FOUND, "permission-not-found", "Permission inexistente");
    }

    @ExceptionHandler(PermissionAlreadyExistsException.class)
    ProblemDetail handlePermissionAlreadyExists() {
        return problem(HttpStatus.CONFLICT, "permission-already-exists", "Ya existe una Permission con ese name");
    }

    @ExceptionHandler(PermissionNameImmutableException.class)
    ProblemDetail handlePermissionNameImmutable() {
        return problem(HttpStatus.BAD_REQUEST, "permission-name-immutable",
                "El name de una Permission es inmutable una vez creada");
    }

    @ExceptionHandler(SystemPermissionProtectedException.class)
    ProblemDetail handleSystemPermissionProtected(SystemPermissionProtectedException e) {
        return problem(HttpStatus.CONFLICT, "system-permission-protected", e.getMessage());
    }

    @ExceptionHandler(AdGroupMappingNotFoundException.class)
    ProblemDetail handleAdGroupMappingNotFound() {
        return problem(HttpStatus.NOT_FOUND, "mapping-not-found", "Mapping AD Group -> Role inexistente");
    }

    @ExceptionHandler(AdGroupMappingAlreadyExistsException.class)
    ProblemDetail handleAdGroupMappingAlreadyExists() {
        return problem(HttpStatus.CONFLICT, "group-already-mapped",
                "Ya existe un mapping para ese grupo de Active Directory");
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    ProblemDetail handlePasswordPolicyViolation() {
        // HttpStatus.UNPROCESSABLE_ENTITY está deprecado desde Spring Framework 7 (sin una
        // constante de reemplazo); se usa el código numérico directamente para el mismo 422.
        return problem(HttpStatus.valueOf(422), "password-policy-violation",
                "La contraseña no cumple la política vigente (mínimo 12 caracteres)");
    }

    // --- Fase 19: huecos genéricos que existían desde que se introdujo este mecanismo (Fase 10) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationFailure(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "invalid-request",
                detail.isBlank() ? "Solicitud inválida" : detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedRequestBody() {
        // No se incluye el mensaje de la excepción subyacente: puede citar literalmente
        // fragmentos del cuerpo recibido, potencialmente sensibles (ej. una contraseña mal
        // escapada en el JSON) — se prefiere un detail genérico.
        return problem(HttpStatus.BAD_REQUEST, "malformed-request-body", "El cuerpo de la solicitud no es JSON válido");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        String correlationId = currentCorrelationId();
        // Log técnico (no SecurityAuditEvent: esto es un fallo técnico, no un hecho de
        // seguridad — ADR-012 distingue ambos canales deliberadamente) con el mismo
        // correlationId que ya comparten los logs y la auditoría de la misma solicitud
        // (REQ-AUTH-024), para poder reconstruirla en un diagnóstico.
        LOG.error("Excepción no anticipada (correlationId={})", correlationId, e);
        // El detail es deliberadamente genérico: nunca se expone el mensaje real ni la clase de
        // la excepción al cliente (no exponer información interna del sistema, mismo principio
        // ya aplicado puntualmente en SPEC-AUTH-007 RN-01, generalizado aquí a cualquier fallo).
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Ocurrió un error inesperado. Contacte a soporte con el correlationId de esta respuesta.");
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(ERROR_BASE_URI + type));
        problemDetail.setProperty("correlationId", currentCorrelationId());
        return problemDetail;
    }

    /** Fijado por {@link CorrelationIdFilter} (Fase 15); {@code null} si, por algún motivo
     * (ej. un test que invoca el controlador sin la cadena de filtros), no se ejecutó. */
    private String currentCorrelationId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        Object attribute = servletAttributes.getRequest().getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        return attribute instanceof String value ? value : null;
    }
}
