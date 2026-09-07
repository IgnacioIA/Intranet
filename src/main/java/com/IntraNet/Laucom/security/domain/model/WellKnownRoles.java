package com.IntraNet.Laucom.security.domain.model;

/**
 * Nombres reservados de {@link Role} de sistema. Ver SPEC-AUTH-001, SPEC-AUTH-009,
 * SPEC-AUTH-010, ADR-019, INV-AUTH-013.
 */
public final class WellKnownRoles {

    public static final String ONBOARDING_USER = "ONBOARDING_USER";
    public static final String MASTER_ADMIN = "MASTER_ADMIN";

    private WellKnownRoles() {
    }
}
