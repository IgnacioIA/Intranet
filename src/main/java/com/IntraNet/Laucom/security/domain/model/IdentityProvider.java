package com.IntraNet.Laucom.security.domain.model;

/**
 * Origen de la identidad de un {@link User}. Ver SPEC-AUTH-001, docs/GLOSSARY.md.
 */
public enum IdentityProvider {
    LOCAL,
    ACTIVE_DIRECTORY
}
