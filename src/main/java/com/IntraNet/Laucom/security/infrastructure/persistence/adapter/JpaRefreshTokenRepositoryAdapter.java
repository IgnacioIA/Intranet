package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.port.RefreshTokenRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;

    public JpaRefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(RefreshTokenPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = jpaRepository.findById(token.id().toString())
                .orElseGet(() -> RefreshTokenPersistenceMapper.toNewJpa(token));
        entity.setRevokedAt(token.revokedAt().orElse(null));
        entity.setReplacedByTokenId(token.replacedByTokenId().map(UUID::toString).orElse(null));
        // saveAndFlush (no solo save): igual criterio que JpaUserRepositoryAdapter (Fase 2) —
        // ante la unicidad de token_hash, falla rápido en la propia operación en vez de diferir
        // el error al flush implícito de otra unidad de trabajo posterior.
        jpaRepository.saveAndFlush(entity);
        return token;
    }

    @Override
    @Transactional
    public void saveRotation(RefreshToken revokedParent, RefreshToken newChild) {
        // Ambas escrituras ocurren dentro de la misma transacción Spring/JPA: o se confirman
        // juntas, o ninguna lo hace (INV-AUTH-006). saveAndFlush fuerza el INSERT del hijo
        // (con su token_hash único) a fallar rápido aquí mismo si algo estuviera mal, en vez de
        // diferirlo al flush implícito de fin de transacción.
        RefreshTokenJpaEntity parentEntity = jpaRepository.findById(revokedParent.id().toString())
                .orElseThrow(() -> new IllegalStateException(
                        "El RefreshToken padre debe existir ya en persistencia antes de rotar"));
        parentEntity.setRevokedAt(revokedParent.revokedAt().orElse(null));
        parentEntity.setReplacedByTokenId(revokedParent.replacedByTokenId().map(UUID::toString).orElse(null));
        jpaRepository.saveAndFlush(parentEntity);

        jpaRepository.saveAndFlush(RefreshTokenPersistenceMapper.toNewJpa(newChild));
    }

    @Override
    @Transactional
    public void revokeAllActiveInFamily(UUID familyId, Instant revokedAt) {
        jpaRepository.revokeAllActiveInFamily(familyId.toString(), revokedAt);
    }

    @Override
    @Transactional
    public void revokeAllActiveForUser(UUID userId, Instant revokedAt) {
        jpaRepository.revokeAllActiveForUser(userId.toString(), revokedAt);
    }
}
