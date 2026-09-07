package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.UserStatus;

/**
 * UC-AUTH-015 SPEC-AUTH-010: filtros y paginación para el listado administrativo de usuarios.
 * Cualquier campo de filtro en {@code null} significa "sin ese filtro". No depende de ningún
 * tipo de Spring Data: el dominio/aplicación no conoce {@code Pageable} — esa traducción vive
 * en el adapter de infraestructura.
 */
public record UserSearchCriteria(UserStatus status, IdentityProvider provider, String searchText,
                                  int page, int size) {
}
