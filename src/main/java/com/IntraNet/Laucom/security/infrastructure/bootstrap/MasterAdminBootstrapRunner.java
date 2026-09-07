package com.IntraNet.Laucom.security.infrastructure.bootstrap;

import com.IntraNet.Laucom.security.application.admin.BootstrapMasterAdminUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * UC-AUTH-014 SPEC-AUTH-009, ADR-019: ejecuta el bootstrap del Master Admin al finalizar el
 * arranque de Spring Boot. Si {@link BootstrapMasterAdminUseCase} lanza (RN-07: no existe
 * administrador y no hay secreto de bootstrap válido), la excepción se propaga sin capturar:
 * {@code SpringApplication.run()} la relanza y el proceso termina — el mecanismo de fail-fast
 * exigido por ADR-019 no es más que dejar que esta excepción no se intercepte en ningún punto.
 */
@Component
@Order(Integer.MIN_VALUE) // primero de todos: ningún otro ApplicationRunner debe ver un estado sin garantía administrativa.
class MasterAdminBootstrapRunner implements ApplicationRunner {

    private final BootstrapMasterAdminUseCase bootstrapMasterAdminUseCase;

    MasterAdminBootstrapRunner(BootstrapMasterAdminUseCase bootstrapMasterAdminUseCase) {
        this.bootstrapMasterAdminUseCase = bootstrapMasterAdminUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapMasterAdminUseCase.handle();
    }
}
