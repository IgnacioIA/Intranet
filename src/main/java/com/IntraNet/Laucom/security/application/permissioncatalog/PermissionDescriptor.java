package com.IntraNet.Laucom.security.application.permissioncatalog;

import java.util.Objects;

/**
 * ADR-021: descriptor declarativo de un {@code Permission} que una aplicación (el propio módulo
 * Security/Auth, o una aplicación consumidora) espera que exista. No es el Value Object de
 * dominio {@code Permission} — es la entrada de configuración que {@link SynchronizePermissionCatalogUseCase}
 * traduce a uno, únicamente cuando todavía no existe en persistencia (regla 1/2 de la ADR).
 */
public record PermissionDescriptor(String code, String description, boolean systemPermission) {

    public PermissionDescriptor {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code no puede estar vacío");
        }
        Objects.requireNonNull(description, "description");
    }
}
