package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.domain.port.PasswordRecoveryTokenRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PasswordRecoveryTokenJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.PasswordRecoveryTokenPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.PasswordRecoveryTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPasswordRecoveryTokenRepositoryAdapter implements PasswordRecoveryTokenRepositoryPort {

    private final PasswordRecoveryTokenJpaRepository repository;

    public JpaPasswordRecoveryTokenRepositoryAdapter(PasswordRecoveryTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasswordRecoveryToken> findUnusedByUserId(UUID userId) {
        return repository.findByUserIdAndUsedAtIsNull(userId.toString()).stream()
                .map(PasswordRecoveryTokenPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PasswordRecoveryTokenPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public PasswordRecoveryToken save(PasswordRecoveryToken token) {
        PasswordRecoveryTokenJpaEntity entity = repository.findById(token.id().toString())
                .orElseGet(() -> PasswordRecoveryTokenPersistenceMapper.toNewJpa(token));
        entity.setUsedAt(token.usedAt().orElse(null));
        repository.save(entity);
        return token;
    }
}
