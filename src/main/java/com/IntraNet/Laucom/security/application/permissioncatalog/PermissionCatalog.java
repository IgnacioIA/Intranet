package com.IntraNet.Laucom.security.application.permissioncatalog;

import java.util.List;

/**
 * ADR-021: puerto de configuración que una aplicación (el propio módulo Security/Auth, o una
 * aplicación consumidora — ej. una futura Intranet) implementa para declarar, junto a su propio
 * código, qué {@link PermissionDescriptor} espera que existan. Se expone como un bean de Spring
 * (interfaz funcional: {@code @Bean public PermissionCatalog xCatalog() { return () -> List.of(...); }}),
 * no como una anotación ni una convención de configuración textual — ver alternativas
 * consideradas en la ADR.
 *
 * <p>El módulo Security/Auth no conoce ni necesita conocer el significado de los descriptores
 * de una aplicación consumidora: solo los persiste si faltan ({@link SynchronizePermissionCatalogUseCase}).</p>
 */
@FunctionalInterface
public interface PermissionCatalog {

    List<PermissionDescriptor> descriptors();
}
