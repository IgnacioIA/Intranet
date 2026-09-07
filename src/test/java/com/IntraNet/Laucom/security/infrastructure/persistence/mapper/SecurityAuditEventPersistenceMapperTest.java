package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.SecurityAuditEventJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditEventPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Test
    void mapsAllFields_andSerializesMetadataAsJson() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        SecurityAuditEvent event = SecurityAuditEvent.occur(id, SecurityEventType.LOGIN_SUCCESS, NOW, actorId,
                subjectId, "corr-1", AuditOutcome.SUCCESS, Map.of("reason", "test"));

        SecurityAuditEventJpaEntity entity = SecurityAuditEventPersistenceMapper.toNewJpa(event);

        assertThat(entity.getId()).isEqualTo(id.toString());
        assertThat(entity.getEventType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(entity.getOccurredAt()).isEqualTo(NOW);
        assertThat(entity.getActorUserId()).isEqualTo(actorId.toString());
        assertThat(entity.getSubjectUserId()).isEqualTo(subjectId.toString());
        assertThat(entity.getCorrelationId()).isEqualTo("corr-1");
        assertThat(entity.getOutcome()).isEqualTo("SUCCESS");
        assertThat(entity.getMetadata()).contains("\"reason\"").contains("\"test\"");
    }

    @Test
    void mapsNullActorAndSubject_toNull() {
        SecurityAuditEvent event = SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.LOGIN_FAILURE, NOW,
                null, null, "corr-1", AuditOutcome.FAILURE, Map.of());

        SecurityAuditEventJpaEntity entity = SecurityAuditEventPersistenceMapper.toNewJpa(event);

        assertThat(entity.getActorUserId()).isNull();
        assertThat(entity.getSubjectUserId()).isNull();
        assertThat(entity.getMetadata()).isEqualTo("{}");
    }
}
