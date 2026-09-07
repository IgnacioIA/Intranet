package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.SecurityAuditEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Solo {@code save} por ahora: ninguna pantalla/consulta de auditoría existe todavía en el
 * módulo (no se anticipan métodos de lectura especulativos, mismo criterio que el resto de los
 * Ports/repositorios de este proyecto).
 */
public interface SecurityAuditEventJpaRepository extends JpaRepository<SecurityAuditEventJpaEntity, String> {
}
