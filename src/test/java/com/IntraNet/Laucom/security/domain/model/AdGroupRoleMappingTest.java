package com.IntraNet.Laucom.security.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cubre entities.md "AdGroupRoleMapping", INV-AUTH-010, SPEC-AUTH-008. */
class AdGroupRoleMappingTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void create_initializesUpdatedFields_equalToCreatedFields() {
        UUID createdBy = UUID.randomUUID();
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                createdBy, NOW);

        assertThat(mapping.createdBy()).isEqualTo(createdBy);
        assertThat(mapping.updatedBy()).isEqualTo(createdBy);
        assertThat(mapping.createdAt()).isEqualTo(NOW);
        assertThat(mapping.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void create_rejectsBlankAdGroupIdentifier() {
        assertThatThrownBy(() -> AdGroupRoleMapping.create(UUID.randomUUID(), "  ", UUID.randomUUID(),
                UUID.randomUUID(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_changesBothFields_andUpdatedTracking() {
        UUID originalRoleId = UUID.randomUUID();
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", originalRoleId,
                UUID.randomUUID(), NOW.minusSeconds(1000));

        UUID newRoleId = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        mapping.update("SALES", newRoleId, updatedBy, NOW);

        assertThat(mapping.adGroupIdentifier()).isEqualTo("SALES");
        assertThat(mapping.roleId()).isEqualTo(newRoleId);
        assertThat(mapping.updatedBy()).isEqualTo(updatedBy);
        assertThat(mapping.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void update_rejectsBlankAdGroupIdentifier() {
        AdGroupRoleMapping mapping = AdGroupRoleMapping.create(UUID.randomUUID(), "IT-SUPPORT", UUID.randomUUID(),
                UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> mapping.update(" ", UUID.randomUUID(), UUID.randomUUID(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstitute_restoresEveryField() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        AdGroupRoleMapping mapping = AdGroupRoleMapping.reconstitute(id, "IT-SUPPORT", roleId, createdBy, updatedBy,
                NOW.minusSeconds(500), NOW);

        assertThat(mapping.id()).isEqualTo(id);
        assertThat(mapping.adGroupIdentifier()).isEqualTo("IT-SUPPORT");
        assertThat(mapping.roleId()).isEqualTo(roleId);
        assertThat(mapping.createdBy()).isEqualTo(createdBy);
        assertThat(mapping.updatedBy()).isEqualTo(updatedBy);
        assertThat(mapping.createdAt()).isEqualTo(NOW.minusSeconds(500));
        assertThat(mapping.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void equality_isBasedOnId() {
        UUID id = UUID.randomUUID();
        AdGroupRoleMapping first = AdGroupRoleMapping.create(id, "IT-SUPPORT", UUID.randomUUID(), UUID.randomUUID(), NOW);
        AdGroupRoleMapping second = AdGroupRoleMapping.create(id, "SALES", UUID.randomUUID(), UUID.randomUUID(), NOW);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
