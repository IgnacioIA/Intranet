package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.RefreshTokenNotActiveException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cubre INV-AUTH-006, INV-AUTH-007, ADR-008 (strict rotation, sin grace period). */
class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void rotate_revokesParent_andProducesChildOfSameFamily() {
        RefreshToken parent = RefreshToken.issueNewFamily(UUID.randomUUID(), UUID.randomUUID(), "hashA", NOW, NOW.plusSeconds(604800));

        RefreshToken child = parent.rotate(UUID.randomUUID(), "hashB", NOW.plusSeconds(10), NOW.plusSeconds(604810));

        assertThat(parent.isRevoked()).isTrue();
        assertThat(parent.replacedByTokenId()).contains(child.id());
        assertThat(child.familyId()).isEqualTo(parent.familyId());
        assertThat(child.isActive(NOW.plusSeconds(11))).isTrue();
    }

    @Test
    void rotate_onAlreadyRevokedToken_throws_signalsReuseToApplicationLayer() {
        RefreshToken token = RefreshToken.issueNewFamily(UUID.randomUUID(), UUID.randomUUID(), "hashA", NOW, NOW.plusSeconds(604800));
        token.rotate(UUID.randomUUID(), "hashB", NOW.plusSeconds(10), NOW.plusSeconds(604810));

        assertThatThrownBy(() -> token.rotate(UUID.randomUUID(), "hashC", NOW.plusSeconds(20), NOW.plusSeconds(604820)))
                .isInstanceOf(RefreshTokenNotActiveException.class);
    }

    @Test
    void expiredToken_isNotActive() {
        RefreshToken token = RefreshToken.issueNewFamily(UUID.randomUUID(), UUID.randomUUID(), "hashA", NOW, NOW.plusSeconds(1));

        assertThat(token.isActive(NOW.plusSeconds(2))).isFalse();
    }
}
