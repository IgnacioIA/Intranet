package com.IntraNet.Laucom.security.domain.model;

/**
 * Nombres reservados de {@link Permission} de sistema. Ver SPEC-AUTH-001 (onboarding).
 */
public final class WellKnownPermissions {

    public static final String VIEW_ONBOARDING_INFO = "VIEW_ONBOARDING_INFO";

    /** SPEC-AUTH-010 §10/§12, sembrados en V5/V8 (Fase 12/17). */
    public static final String USER_READ = "USER_READ";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_MANAGE = "PERMISSION_MANAGE";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String ROLE_REVOKE = "ROLE_REVOKE";
    /** SPEC-AUTH-008, sembrado en V5 (Fase 12). */
    public static final String AD_MAPPING_MANAGE = "AD_MAPPING_MANAGE";

    private WellKnownPermissions() {
    }
}
