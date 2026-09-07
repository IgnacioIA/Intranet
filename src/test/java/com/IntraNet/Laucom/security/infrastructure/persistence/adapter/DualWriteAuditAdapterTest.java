package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.IntraNet.Laucom.security.domain.model.AuditOutcome;
import com.IntraNet.Laucom.security.domain.model.SecurityAuditEvent;
import com.IntraNet.Laucom.security.domain.model.SecurityEventType;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.SecurityAuditEventJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.SecurityAuditEventJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ADR-012: verifica los dos canales de forma independiente, incluida la garantía de redundancia
 * (un fallo en un canal no debe impedir el otro).
 */
@ExtendWith(MockitoExtension.class)
class DualWriteAuditAdapterTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private SecurityAuditEventJpaRepository repository;

    private DualWriteAuditAdapter adapter;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        adapter = new DualWriteAuditAdapter(repository);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger("SECURITY_AUDIT")).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("SECURITY_AUDIT")).detachAppender(logAppender);
    }

    private static SecurityAuditEvent anEvent() {
        return SecurityAuditEvent.occur(UUID.randomUUID(), SecurityEventType.LOGIN_SUCCESS, NOW,
                UUID.randomUUID(), UUID.randomUUID(), "corr-1", AuditOutcome.SUCCESS, Map.of("reason", "ok"));
    }

    @Test
    void writesToDatabase_andToTheStructuredLog() {
        SecurityAuditEvent event = anEvent();

        adapter.record(event);

        ArgumentCaptor<SecurityAuditEventJpaEntity> captor = ArgumentCaptor.forClass(SecurityAuditEventJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(event.id().toString());

        assertThat(logAppender.list).hasSize(1);
        String logLine = logAppender.list.get(0).getFormattedMessage();
        assertThat(logLine).contains("\"eventType\":\"LOGIN_SUCCESS\"").contains(event.id().toString());
    }

    @Test
    void aDatabaseFailure_doesNotPreventTheStructuredLogWrite() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB down"));

        adapter.record(anEvent()); // no debe lanzar (ADR-012: canales independientes).

        assertThat(logAppender.list).hasSize(1);
    }
}
