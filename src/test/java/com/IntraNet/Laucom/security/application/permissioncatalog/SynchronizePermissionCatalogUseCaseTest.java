package com.IntraNet.Laucom.security.application.permissioncatalog;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre ADR-021 (Permission Catalog): reglas de sincronización declarativa al arranque.
 */
@ExtendWith(MockitoExtension.class)
class SynchronizePermissionCatalogUseCaseTest {

    @Mock
    private PermissionRepositoryPort permissionRepository;

    private SynchronizePermissionCatalogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SynchronizePermissionCatalogUseCase(permissionRepository);
    }

    @Test
    void newDescriptor_absentFromDb_isCreatedActive() {
        when(permissionRepository.findByName("INTRANET.DOCUMENT_READ")).thenReturn(Optional.empty());
        PermissionCatalog catalog = () -> List.of(
                new PermissionDescriptor("INTRANET.DOCUMENT_READ", "Leer documentos", false));

        useCase.handle(List.of(catalog));

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("INTRANET.DOCUMENT_READ");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().isSystemPermission()).isFalse();
    }

    @Test
    void existingActiveDescriptor_isNeverTouched() {
        Permission existing = Permission.create("INTRANET.DOCUMENT_READ", "Leer documentos");
        when(permissionRepository.findByName("INTRANET.DOCUMENT_READ")).thenReturn(Optional.of(existing));
        PermissionCatalog catalog = () -> List.of(
                new PermissionDescriptor("INTRANET.DOCUMENT_READ", "Descripción distinta", false));

        useCase.handle(List.of(catalog));

        verify(permissionRepository, never()).save(any());
    }

    @Test
    void existingDeactivatedDescriptor_isNeverReactivated() {
        Permission deactivated = Permission.create("INTRANET.REPORT_EXPORT", "Exportar reportes");
        deactivated.deactivate();
        when(permissionRepository.findByName("INTRANET.REPORT_EXPORT")).thenReturn(Optional.of(deactivated));
        PermissionCatalog catalog = () -> List.of(
                new PermissionDescriptor("INTRANET.REPORT_EXPORT", "Exportar reportes", false));

        useCase.handle(List.of(catalog));

        verify(permissionRepository, never()).save(any());
        assertThat(deactivated.isActive()).isFalse(); // la decisión administrativa se preserva.
    }

    @Test
    void runningTwice_neverCreatesADuplicate() {
        PermissionDescriptor descriptor = new PermissionDescriptor("INTRANET.DOCUMENT_READ", "Leer documentos", false);
        PermissionCatalog catalog = () -> List.of(descriptor);
        when(permissionRepository.findByName("INTRANET.DOCUMENT_READ"))
                .thenReturn(Optional.empty()) // primera ejecución: no existe.
                .thenReturn(Optional.of(Permission.create("INTRANET.DOCUMENT_READ", "Leer documentos"))); // segunda: ya existe.

        useCase.handle(List.of(catalog));
        useCase.handle(List.of(catalog));

        verify(permissionRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void systemPermission_isPersistedAsSystemPermission() {
        when(permissionRepository.findByName("AD_MAPPING_MANAGE")).thenReturn(Optional.empty());
        PermissionCatalog catalog = () -> List.of(
                new PermissionDescriptor("AD_MAPPING_MANAGE", "Administración del mapping AD", true));

        useCase.handle(List.of(catalog));

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        assertThat(captor.getValue().isSystemPermission()).isTrue();
    }

    @Test
    void multipleCatalogs_areAllSynchronized() {
        when(permissionRepository.findByName(any())).thenReturn(Optional.empty());
        PermissionCatalog first = () -> List.of(new PermissionDescriptor("AUTH_PERM", "desc", true));
        PermissionCatalog second = () -> List.of(new PermissionDescriptor("INTRANET.DOCUMENT_READ", "desc", false));

        useCase.handle(List.of(first, second));

        verify(permissionRepository, org.mockito.Mockito.times(2)).save(any());
    }
}
