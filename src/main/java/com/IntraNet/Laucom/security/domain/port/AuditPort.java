package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;

/**
 * Registro de Security Audit. Ver ADR-012: doble escritura (DB + log estructurado) oculta
 * tras este Port; el dominio no depende de este puerto directamente (lo consume la capa de
 * aplicación a partir de eventos, según architecture.md §4).
 */
public interface AuditPort {

    void record(SecurityAuditEvent event);
}
