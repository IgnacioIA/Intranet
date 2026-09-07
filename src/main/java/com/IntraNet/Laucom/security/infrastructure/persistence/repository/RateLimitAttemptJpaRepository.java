package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RateLimitAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RateLimitAttemptJpaRepository extends JpaRepository<RateLimitAttemptJpaEntity, String> {

    long countByRateKeyAndAttemptedAtAfter(String rateKey, Instant windowStart);
}
