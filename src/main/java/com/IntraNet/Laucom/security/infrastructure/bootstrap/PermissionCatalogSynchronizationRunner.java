package com.IntraNet.Laucom.security.infrastructure.bootstrap;

import com.IntraNet.Laucom.security.application.permissioncatalog.PermissionCatalog;
import com.IntraNet.Laucom.security.application.permissioncatalog.SynchronizePermissionCatalogUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ADR-021: sincroniza, al arrancar, todo bean {@link PermissionCatalog} presente en el contexto
 * de Spring — el del propio módulo Security/Auth ({@code SecurityModulePermissionCatalogConfig})
 * y el de cualquier aplicación consumidora que declare el suyo. Spring inyecta automáticamente
 * la lista completa de beans de ese tipo (orden de declaración, sin significado semántico: la
 * sincronización de cada descriptor es independiente y conmutativa).
 *
 * <p>Corre después del bootstrap del Master Admin (que tiene prioridad máxima,
 * {@code Integer.MIN_VALUE}): ningún Use Case de esta fase de arranque depende de que un
 * permiso declarado aquí ya exista (los permisos que sí necesita el Master Admin desde el
 * primer request están garantizados por las migraciones Flyway V5/V8/V9, que corren antes de
 * cualquier {@code ApplicationRunner} — ver Javadoc de {@code SecurityModulePermissionCatalogConfig}).</p>
 */
@Component
@Order(Integer.MIN_VALUE + 1)
class PermissionCatalogSynchronizationRunner implements ApplicationRunner {

    private final SynchronizePermissionCatalogUseCase synchronizePermissionCatalog;
    private final List<PermissionCatalog> catalogs;

    PermissionCatalogSynchronizationRunner(SynchronizePermissionCatalogUseCase synchronizePermissionCatalog,
                                            List<PermissionCatalog> catalogs) {
        this.synchronizePermissionCatalog = synchronizePermissionCatalog;
        this.catalogs = catalogs;
    }

    @Override
    public void run(ApplicationArguments args) {
        synchronizePermissionCatalog.handle(catalogs);
    }
}
