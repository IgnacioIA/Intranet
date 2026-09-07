package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/** SPEC-AUTH-010 §12: alta/modificación de Roles. {@code name} se ignora en modificación (RN-10 no aplica a Role, pero
 * el endpoint de modificación tampoco lo usa: se identifica por {@code roleId} en la ruta). */
public record CreateOrUpdateRoleRequest(@NotBlank String name, String description, Set<String> permissions) {
}
