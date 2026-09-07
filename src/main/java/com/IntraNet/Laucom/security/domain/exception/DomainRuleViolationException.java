package com.IntraNet.Laucom.security.domain.exception;

/**
 * Base de las excepciones que representan una violación de una regla de negocio/invariante
 * ya documentada (RN-XX / INV-AUTH-XX). No representa errores técnicos de infraestructura.
 */
public abstract class DomainRuleViolationException extends RuntimeException {

    protected DomainRuleViolationException(String message) {
        super(message);
    }
}
