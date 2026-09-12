package com.IntraNet.Laucom.security.application.exception;

/** SPEC-AUTH-008 §12. 404 {@code mapping-not-found}. */
public final class AdGroupMappingNotFoundException extends ApplicationRuleViolationException {

    public AdGroupMappingNotFoundException() {
        super("Mapping AD Group -> Role inexistente");
    }
}
