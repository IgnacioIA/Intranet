package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RefreshTokenJpaEntity;

import java.util.UUID;

public final class RefreshTokenPersistenceMapper {

    private RefreshTokenPersistenceMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.reconstitute(UUID.fromString(entity.getId()), UUID.fromString(entity.getUserId()),
                entity.getTokenHash(), UUID.fromString(entity.getFamilyId()), entity.getIssuedAt(),
                entity.getExpiresAt(), entity.getRevokedAt(),
                entity.getReplacedByTokenId() == null ? null : UUID.fromString(entity.getReplacedByTokenId()));
    }

    public static RefreshTokenJpaEntity toNewJpa(RefreshToken token) {
        return new RefreshTokenJpaEntity(token.id().toString(), token.userId().toString(), token.tokenHash(),
                token.familyId().toString(), token.issuedAt(), token.expiresAt(),
                token.revokedAt().orElse(null),
                token.replacedByTokenId().map(UUID::toString).orElse(null));
    }
}
