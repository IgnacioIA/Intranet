package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PasswordRecoveryTokenJpaEntity;

import java.util.UUID;

public final class PasswordRecoveryTokenPersistenceMapper {

    private PasswordRecoveryTokenPersistenceMapper() {
    }

    public static PasswordRecoveryTokenJpaEntity toNewJpa(PasswordRecoveryToken token) {
        return new PasswordRecoveryTokenJpaEntity(token.id().toString(), token.userId().toString(),
                token.tokenHash(), token.issuedAt(), token.expiresAt(), token.usedAt().orElse(null));
    }

    public static PasswordRecoveryToken toDomain(PasswordRecoveryTokenJpaEntity entity) {
        return PasswordRecoveryToken.reconstitute(UUID.fromString(entity.getId()), UUID.fromString(entity.getUserId()),
                entity.getTokenHash(), entity.getIssuedAt(), entity.getExpiresAt(), entity.getUsedAt());
    }
}
