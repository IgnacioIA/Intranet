package com.IntraNet.Laucom.security.application.exception;

/**
 * Base de las excepciones que representan el rechazo de una operación por una regla de
 * orquestación de un Use Case (distinto de una violación de invariante de una única entidad,
 * que usa {@link com.IntraNet.Laucom.security.domain.exception.DomainRuleViolationException}).
 */
public abstract class ApplicationRuleViolationException extends RuntimeException {

    protected ApplicationRuleViolationException(String message) {
        super(message);
    }
}
