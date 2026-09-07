package com.IntraNet.Laucom.security.infrastructure.rest.dto;

/** SPEC-AUTH-003 §12: cuerpo de respuesta común a logout y logout-all. */
public record StatusResponse(String status) {

    public static StatusResponse ok() {
        return new StatusResponse("ok");
    }
}
