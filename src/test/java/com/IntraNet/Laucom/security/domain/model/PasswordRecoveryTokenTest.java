package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.RecoveryTokenInvalidException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cubre INV-AUTH-008. */
class PasswordRecoveryTokenTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void markUsed_twice_throws() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(3600));
        token.markUsed(NOW.plusSeconds(10));

        assertThatThrownBy(() -> token.markUsed(NOW.plusSeconds(20)))
                .isInstanceOf(RecoveryTokenInvalidException.class);
    }

    @Test
    void markUsed_afterExpiration_throws() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));

        assertThatThrownBy(() -> token.markUsed(NOW.plusSeconds(61)))
                .isInstanceOf(RecoveryTokenInvalidException.class);
    }

    @Test
    void isValid_falseOnceUsed() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(3600));
        token.markUsed(NOW.plusSeconds(10));

        assertThat(token.isValid(NOW.plusSeconds(20))).isFalse();
    }

    @Test
    void invalidate_marksTokenUnusable_withoutRequiringConfirmation_RN06() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(3600));

        token.invalidate(NOW.plusSeconds(5));

        assertThat(token.isValid(NOW.plusSeconds(6))).isFalse();
        assertThat(token.usedAt()).isPresent();
    }

    @Test
    void invalidate_onAlreadyExpiredToken_doesNotThrow() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));

        token.invalidate(NOW.plusSeconds(1000)); // ya expirado; invalidate no debe lanzar (a diferencia de markUsed)

        assertThat(token.isValid(NOW.plusSeconds(1001))).isFalse();
    }

    @Test
    void invalidate_isIdempotent() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(3600));

        token.invalidate(NOW.plusSeconds(5));
        token.invalidate(NOW.plusSeconds(10)); // no debe lanzar ni cambiar el usedAt original

        assertThat(token.usedAt()).contains(NOW.plusSeconds(5));
    }
}
