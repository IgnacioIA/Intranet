package com.IntraNet.Laucom.security.infrastructure.rest;

/**
 * SPEC-AUTH-001 §12 / RN-08 / ADR-018: {@code provider} ausente o con un valor distinto de
 * {@code LOCAL}/{@code ACTIVE_DIRECTORY}. Es una validación de forma de la solicitud HTTP, no
 * una regla de negocio de un Use Case — por eso vive en el borde (`infrastructure.rest`), no en
 * `application.exception`.
 */
public class InvalidProviderException extends RuntimeException {

    public InvalidProviderException() {
        super("provider debe ser LOCAL o ACTIVE_DIRECTORY");
    }
}
