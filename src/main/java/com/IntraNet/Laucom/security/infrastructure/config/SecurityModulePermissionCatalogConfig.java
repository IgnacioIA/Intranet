package com.IntraNet.Laucom.security.infrastructure.config;

import com.IntraNet.Laucom.security.application.permissioncatalog.PermissionCatalog;
import com.IntraNet.Laucom.security.application.permissioncatalog.PermissionDescriptor;
import com.IntraNet.Laucom.security.domain.model.WellKnownPermissions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ADR-021: el propio módulo Security/Auth declara sus permisos administrativos a través del
 * mismo mecanismo de {@link PermissionCatalog} que usaría cualquier aplicación consumidora. En
 * la práctica, todos ellos ya existen en persistencia desde las migraciones V5/V8/V9 — esta
 * declaración es un no-op en cada arranque (regla 2 de la ADR: nunca se toca lo ya existente) y
 * sirve como demostración/documentación-como-código del catálogo completo, no como su única
 * fuente real.
 *
 * <p>{@code systemPermission = false} para los once, igual que ya están sembrados: son permisos
 * administrables como cualquier otro {@code Permission} de negocio (RN-09 SPEC-AUTH-010 solo
 * protege incondicionalmente a un permiso realmente marcado como de sistema, del cual
 * actualmente ninguno de esta familia lo es). {@code VIEW_ONBOARDING_INFO} deliberadamente NO se
 * declara aquí: es un permiso virtual sintetizado en memoria por {@code GetCurrentUserUseCase}
 * para la respuesta de un usuario {@code PENDING_ONBOARDING} — nunca tuvo, ni debe tener, una
 * fila persistida en {@code permissions} (crearla cambiaría ese comportamiento ya aprobado).</p>
 */
@Configuration
public class SecurityModulePermissionCatalogConfig {

    @Bean
    public PermissionCatalog securityModulePermissionCatalog() {
        return () -> List.of(
                new PermissionDescriptor(WellKnownPermissions.USER_READ,
                        "Consulta de usuarios, lista y detalle (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.USER_MANAGE,
                        "Alta, actualización y cambios de estado de usuarios LOCAL (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.ROLE_READ,
                        "Lectura de Roles (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.ROLE_MANAGE,
                        "Alta, modificación y activación/desactivación de Roles (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.PERMISSION_READ,
                        "Lectura de Permissions (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.PERMISSION_MANAGE,
                        "Alta, modificación y activación/desactivación de Permissions (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.ROLE_ASSIGN,
                        "Asignación explícita de un Role a un usuario (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.ROLE_REVOKE,
                        "Revocación de una asignación GRANTED_EXPLICITLY (SPEC-AUTH-010)", false),
                new PermissionDescriptor(WellKnownPermissions.AD_MAPPING_MANAGE,
                        "Administración del mapping AD Group -> Role (SPEC-AUTH-008)", false),
                new PermissionDescriptor(WellKnownPermissions.SESSION_REVOKE_ANY,
                        "Revocación administrativa de las sesiones de un usuario (SPEC-AUTH-004)", false));
    }
}
