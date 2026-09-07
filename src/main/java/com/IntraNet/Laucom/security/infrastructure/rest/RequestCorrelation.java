package com.IntraNet.Laucom.security.infrastructure.rest;

import com.IntraNet.Laucom.security.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/** Lee el {@code correlationId} fijado por {@link CorrelationIdFilter} (Fase 15). */
final class RequestCorrelation {

    private RequestCorrelation() {
    }

    static String from(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        return attribute instanceof String value ? value : UUID.randomUUID().toString();
    }
}
