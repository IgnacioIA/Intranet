package com.IntraNet.Laucom.security.infrastructure.directory;

import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, a nivel de wiring de Spring (sin depender de MySQL/Flyway/JWT ni de un Active
 * Directory real — ver ADR-002 y el Javadoc de {@link ActiveDirectoryAdapter} sobre la
 * imposibilidad de verificarlo contra AD real en este entorno), que exactamente un bean de
 * {@link IdentityDirectoryPort} existe para cualquier valor de {@code ad.enabled}, y que
 * {@link ActiveDirectoryAdapter} y {@link NoOpIdentityDirectoryAdapter} son mutuamente
 * excluyentes.
 *
 * <p>Usa {@link ApplicationContextRunner} en vez de {@code @SpringBootTest} deliberadamente: el
 * problema a demostrar es puramente de resolución de {@code @ConditionalOnProperty} sobre estas
 * dos clases, no el arranque completo de la aplicación (eso ya lo cubre
 * {@code LaucomApplicationTests}, con {@code ad.enabled} en su valor por defecto). Evita así
 * depender de un datasource real solo para verificar una condición de registro de bean.</p>
 */
class IdentityDirectoryPortConditionalWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(PropertySourcesPlaceholderConfigurer.class)
            .withUserConfiguration(ActiveDirectoryAdapter.class, NoOpIdentityDirectoryAdapter.class);

    @Test
    void adEnabledFalse_registersOnlyTheNoOpAdapter() {
        contextRunner.withPropertyValues("ad.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(IdentityDirectoryPort.class);
            assertThat(context).hasSingleBean(NoOpIdentityDirectoryAdapter.class);
            assertThat(context).doesNotHaveBean(ActiveDirectoryAdapter.class);
        });
    }

    @Test
    void adEnabledAbsent_defaultsToTheNoOpAdapter() {
        // Sin "ad.enabled" en absoluto: el mismo escenario que application.properties describe
        // como default (${AD_ENABLED:false}) cuando ni siquiera se definió la variable de entorno.
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdentityDirectoryPort.class);
            assertThat(context).hasSingleBean(NoOpIdentityDirectoryAdapter.class);
            assertThat(context).doesNotHaveBean(ActiveDirectoryAdapter.class);
        });
    }

    @Test
    void adEnabledTrue_registersOnlyTheActiveDirectoryAdapter() {
        // Valores dummy: solo necesarios para que se resuelvan los @Value del constructor de
        // ActiveDirectoryAdapter. Este test no invoca authenticate(...) ni abre ninguna conexión
        // LDAP — verifica únicamente qué bean queda registrado.
        contextRunner.withPropertyValues(
                "ad.enabled=true",
                "ad.url=",
                "ad.base-dn=",
                "ad.domain=",
                "ad.service-account.principal=",
                "ad.service-account.password="
        ).run(context -> {
            assertThat(context).hasSingleBean(IdentityDirectoryPort.class);
            assertThat(context).hasSingleBean(ActiveDirectoryAdapter.class);
            assertThat(context).doesNotHaveBean(NoOpIdentityDirectoryAdapter.class);
        });
    }
}
