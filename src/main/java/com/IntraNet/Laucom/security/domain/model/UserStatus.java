package com.IntraNet.Laucom.security.domain.model;

/**
 * Estados de {@link User}. Ver docs/02-domain/states.md y docs/02-domain/transitions.md.
 */
public enum UserStatus {
    PENDING_ONBOARDING,
    ACTIVE,
    LOCKED,
    DISABLED,
    DEPROVISIONED
}
