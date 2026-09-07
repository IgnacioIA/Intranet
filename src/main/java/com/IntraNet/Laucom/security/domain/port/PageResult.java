package com.IntraNet.Laucom.security.domain.port;

import java.util.List;

/** Resultado paginado genérico, sin depender de ningún tipo de Spring Data. */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
}
