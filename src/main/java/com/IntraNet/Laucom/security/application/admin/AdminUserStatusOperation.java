package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.domain.model.SecurityEventType;

/** UC-AUTH-018 SPEC-AUTH-010: la propia SPEC describe un único flujo parametrizado por operación. */
public enum AdminUserStatusOperation {

    ENABLE(SecurityEventType.USER_ENABLED),
    DISABLE(SecurityEventType.USER_DISABLED),
    LOCK(SecurityEventType.USER_LOCKED),
    UNLOCK(SecurityEventType.USER_UNLOCKED),
    DEPROVISION(SecurityEventType.USER_DEPROVISIONED);

    private final SecurityEventType auditEventType;

    AdminUserStatusOperation(SecurityEventType auditEventType) {
        this.auditEventType = auditEventType;
    }

    public SecurityEventType auditEventType() {
        return auditEventType;
    }

    /** RN-07 SPEC-AUTH-010: estas tres operaciones pueden dejar al usuario sin poder operar. */
    public boolean requiresMasterAdminContinuityCheck() {
        return this == DISABLE || this == LOCK || this == DEPROVISION;
    }

    /** RN-04 SPEC-AUTH-010: estas tres revocan las sesiones activas del usuario. */
    public boolean revokesSessions() {
        return this == DISABLE || this == LOCK || this == DEPROVISION;
    }
}
