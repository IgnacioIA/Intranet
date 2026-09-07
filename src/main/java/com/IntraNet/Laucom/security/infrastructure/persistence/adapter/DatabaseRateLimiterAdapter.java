package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RateLimitAttemptJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.RateLimitAttemptJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * ADR-013: adapter respaldado por base de datos (ventana deslizante mediante un log de
 * intentos), válido en despliegue multi-instancia (REQ-AUTH-022) sin introducir Redis en V1.
 * Migrar a un adapter Redis futuro, de ser necesario, no requiere cambios en el dominio ni en la
 * aplicación — solo una nueva implementación de este mismo Port.
 */
@Component
public class DatabaseRateLimiterAdapter implements RateLimiterPort {

    private final RateLimitAttemptJpaRepository repository;
    private final Clock clock;

    public DatabaseRateLimiterAdapter(RateLimitAttemptJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RateLimitDecision checkAndRecord(String key, int maxAttempts, Duration window) {
        Instant now = clock.instant();
        // Se registra el intento antes de contar: cada llamada representa un intento real que
        // ocurrió, incluido el que eventualmente resulte denegado (log de intentos, no solo de
        // los permitidos) — así la ventana deslizante refleja la actividad real contra la clave.
        repository.save(new RateLimitAttemptJpaEntity(UUID.randomUUID().toString(), key, now));

        Instant windowStart = now.minus(window);
        long attemptsInWindow = repository.countByRateKeyAndAttemptedAtAfter(key, windowStart);

        boolean allowed = attemptsInWindow <= maxAttempts;
        int remaining = (int) Math.max(0, maxAttempts - attemptsInWindow);
        return new RateLimitDecision(allowed, remaining);
    }
}
