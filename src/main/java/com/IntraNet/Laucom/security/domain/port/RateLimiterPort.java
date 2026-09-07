package com.IntraNet.Laucom.security.domain.port;

import java.time.Duration;

/**
 * Ver ADR-013: adapter de base de datos en V1, preparado para un adapter distribuido (Redis)
 * futuro sin modificar el dominio. REQ-AUTH-018, REQ-AUTH-022.
 */
public interface RateLimiterPort {

    RateLimitDecision checkAndRecord(String key, int maxAttempts, Duration window);

    record RateLimitDecision(boolean allowed, int remainingAttempts) {
    }
}
