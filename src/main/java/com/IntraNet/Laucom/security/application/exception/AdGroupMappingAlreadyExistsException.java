package com.IntraNet.Laucom.security.application.exception;

/** UC-AUTH-013 SPEC-AUTH-008 RN-02, 409 {@code group-already-mapped}. */
public final class AdGroupMappingAlreadyExistsException extends ApplicationRuleViolationException {

    public AdGroupMappingAlreadyExistsException() {
        super("Ya existe un mapping para ese grupo de Active Directory");
    }
}
