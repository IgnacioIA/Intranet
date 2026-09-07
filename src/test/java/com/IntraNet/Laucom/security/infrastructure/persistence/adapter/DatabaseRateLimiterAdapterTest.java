package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.port.RateLimiterPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RateLimitAttemptJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.RateLimitAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADR-013: ventana deslizante respaldada por un log de intentos, verificada con el repositorio mockeado. */
@ExtendWith(MockitoExtension.class)
class DatabaseRateLimiterAdapterTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Mock
    private RateLimitAttemptJpaRepository repository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private DatabaseRateLimiterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DatabaseRateLimiterAdapter(repository, clock);
    }

    @Test
    void recordsTheAttempt_beforeCounting() {
        when(repository.countByRateKeyAndAttemptedAtAfter(eq("login:ip:10.0.0.1"), any())).thenReturn(1L);

        adapter.checkAndRecord("login:ip:10.0.0.1", 20, WINDOW);

        ArgumentCaptor<RateLimitAttemptJpaEntity> captor = ArgumentCaptor.forClass(RateLimitAttemptJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRateKey()).isEqualTo("login:ip:10.0.0.1");
        assertThat(captor.getValue().getAttemptedAt()).isEqualTo(NOW);
    }

    @Test
    void countsWithinTheExactWindowStart() {
        when(repository.countByRateKeyAndAttemptedAtAfter(any(), any())).thenReturn(1L);

        adapter.checkAndRecord("login:ip:10.0.0.1", 20, WINDOW);

        verify(repository).countByRateKeyAndAttemptedAtAfter("login:ip:10.0.0.1", NOW.minus(WINDOW));
    }

    @Test
    void allowsWhenAttemptsAreAtOrBelowTheLimit() {
        when(repository.countByRateKeyAndAttemptedAtAfter(any(), any())).thenReturn(5L);

        RateLimiterPort.RateLimitDecision decision = adapter.checkAndRecord("k", 5, WINDOW);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remainingAttempts()).isZero();
    }

    @Test
    void deniesWhenAttemptsExceedTheLimit() {
        when(repository.countByRateKeyAndAttemptedAtAfter(any(), any())).thenReturn(6L);

        RateLimiterPort.RateLimitDecision decision = adapter.checkAndRecord("k", 5, WINDOW);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remainingAttempts()).isZero();
    }

    @Test
    void reportsRemainingAttemptsWhenWellBelowTheLimit() {
        when(repository.countByRateKeyAndAttemptedAtAfter(any(), any())).thenReturn(3L);

        RateLimiterPort.RateLimitDecision decision = adapter.checkAndRecord("k", 20, WINDOW);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remainingAttempts()).isEqualTo(17);
    }
}
