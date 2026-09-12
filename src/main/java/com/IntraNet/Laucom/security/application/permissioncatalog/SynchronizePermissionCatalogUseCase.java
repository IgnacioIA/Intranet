package com.IntraNet.Laucom.security.application.permissioncatalog;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ADR-021: ejecutado una única vez al arranque (ver {@code PermissionCatalogSynchronizationRunner}),
 * sin actor autenticado — igual excepción legítima que {@code BootstrapMasterAdminUseCase} a la
 * regla de que todo Use Case administrativo pasa por {@code AdminActionAuthorizer} (por eso vive
 * en un paquete propio, {@code application.permissioncatalog}, fuera de {@code application.admin}
 * y de su regla ArchUnit).
 *
 * <p>Reglas (Decision Ledger 2026-09-07, ver ADR-021 §5): un descriptor ausente en DB se crea,
 * {@code active = true}; un descriptor ya existente NUNCA se modifica, reactiva ni elimina —
 * la decisión administrativa vigente siempre prevalece sobre la declaración de arranque.</p>
 */
@Service
public class SynchronizePermissionCatalogUseCase {

    private final PermissionRepositoryPort permissionRepository;

    public SynchronizePermissionCatalogUseCase(PermissionRepositoryPort permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public void handle(List<PermissionCatalog> catalogs) {
        for (PermissionCatalog catalog : catalogs) {
            for (PermissionDescriptor descriptor : catalog.descriptors()) {
                synchronize(descriptor);
            }
        }
    }

    private void synchronize(PermissionDescriptor descriptor) {
        if (permissionRepository.findByName(descriptor.code()).isPresent()) {
            return; // Regla 2: ya existe — no se crea duplicado, no se modifica, no se reactiva.
        }
        Permission permission = descriptor.systemPermission()
                ? Permission.createSystemPermission(descriptor.code(), descriptor.description())
                : Permission.create(descriptor.code(), descriptor.description());
        permissionRepository.save(permission);
    }
}
