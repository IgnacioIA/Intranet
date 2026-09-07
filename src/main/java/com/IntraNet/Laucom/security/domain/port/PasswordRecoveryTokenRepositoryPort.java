package com.IntraNet.Laucom.security.domain.port;

import com.IntraNet.Laucom.security.domain.model.PasswordRecoveryToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de {@link PasswordRecoveryToken}. Ver SPEC-AUTH-007, INV-AUTH-008.
 */
public interface PasswordRecoveryTokenRepositoryPort {

    /** RN-06 SPEC-AUTH-007: tokens no usados de un usuario, para invalidarlos al emitir uno nuevo. */
    List<PasswordRecoveryToken> findUnusedByUserId(UUID userId);

    Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash);

    PasswordRecoveryToken save(PasswordRecoveryToken token);
}
