package com.IntraNet.Laucom.security.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SPEC-AUTH-001 §12. {@code provider} es obligatorio (ADR-018): el sistema no autodetecta el
 * proveedor a partir de {@code username}.
 */
public record LoginRequest(@NotBlank String provider, @NotBlank String username, @NotBlank String password) {
}
