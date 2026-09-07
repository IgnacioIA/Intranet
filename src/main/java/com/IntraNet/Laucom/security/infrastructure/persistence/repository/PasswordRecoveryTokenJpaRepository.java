package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PasswordRecoveryTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordRecoveryTokenJpaRepository extends JpaRepository<PasswordRecoveryTokenJpaEntity, String> {

    Optional<PasswordRecoveryTokenJpaEntity> findByTokenHash(String tokenHash);

    List<PasswordRecoveryTokenJpaEntity> findByUserIdAndUsedAtIsNull(String userId);
}
