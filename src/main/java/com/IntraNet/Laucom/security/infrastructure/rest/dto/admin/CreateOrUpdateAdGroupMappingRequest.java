package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;

/** SPEC-AUTH-008 §12: alta/modificación de `AdGroupRoleMapping`. */
public record CreateOrUpdateAdGroupMappingRequest(@NotBlank String adGroupIdentifier, @NotBlank String roleId) {
}
