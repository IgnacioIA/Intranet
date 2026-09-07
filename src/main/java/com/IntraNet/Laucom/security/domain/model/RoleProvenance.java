package com.IntraNet.Laucom.security.domain.model;

/**
 * Procedencia de un {@link UserRoleAssignment}. Ver REQ-AUTH-007, INV-AUTH-005, INV-AUTH-015.
 */
public enum RoleProvenance {
    DERIVED_FROM_AD,
    GRANTED_EXPLICITLY
}
