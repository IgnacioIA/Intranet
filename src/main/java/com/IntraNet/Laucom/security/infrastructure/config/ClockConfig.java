package com.IntraNet.Laucom.security.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * {@link Clock} centralizado (UTC — REQ-AUTH-024 exige timestamps UTC en auditoría) para que
 * los Use Cases sean determinísticamente testeables sin llamar a {@code Instant.now()} directo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
