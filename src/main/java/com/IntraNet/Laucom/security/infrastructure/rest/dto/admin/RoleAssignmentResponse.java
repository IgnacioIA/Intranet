package com.IntraNet.Laucom.security.infrastructure.rest.dto.admin;

import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;

/** SPEC-AUTH-010 §12: `GET /auth/admin/users/{userId}/roles` y respuesta de asignar/consultar. */
public record RoleAssignmentResponse(String roleId, String provenance, String sourceAdGroup) {

    public static RoleAssignmentResponse from(UserRoleAssignment assignment) {
        return new RoleAssignmentResponse(assignment.roleId().toString(), assignment.provenance().name(),
                assignment.sourceAdGroup());
    }
}
