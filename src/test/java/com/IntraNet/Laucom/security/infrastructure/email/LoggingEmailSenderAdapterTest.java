package com.IntraNet.Laucom.security.infrastructure.email;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-AUTH-023 / testing-strategy.md §3 ("ausencia de secretos en logs"): el placeholder de
 * {@code EmailSenderPort} nunca debe escribir el token de recuperación en claro en el log de
 * aplicación, sin importar cuán "obvio" o corto sea el valor.
 */
class LoggingEmailSenderAdapterTest {

    private static final String RECOVERY_TOKEN_PLAIN_VALUE = "super-secret-recovery-token-value";

    private final LoggingEmailSenderAdapter adapter = new LoggingEmailSenderAdapter();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(LoggingEmailSenderAdapter.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(LoggingEmailSenderAdapter.class)).detachAppender(logAppender);
    }

    @Test
    void neverLogsThePlainRecoveryToken() {
        adapter.sendPasswordRecoveryEmail("jdoe@example.com", RECOVERY_TOKEN_PLAIN_VALUE);

        assertThat(logAppender.list).isNotEmpty();
        for (ILoggingEvent event : logAppender.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(RECOVERY_TOKEN_PLAIN_VALUE);
        }
    }

    @Test
    void neverLogsTheRecipientEmail() {
        // No es un secreto en el mismo sentido, pero tampoco aporta valor en un log técnico de
        // "placeholder" y sería un dato personal innecesario en un canal que no es el Security
        // Audit (que sí registra el actor/subject por UUID, nunca el email en claro).
        adapter.sendPasswordRecoveryEmail("jdoe@example.com", RECOVERY_TOKEN_PLAIN_VALUE);

        assertThat(logAppender.list).isNotEmpty();
        for (ILoggingEvent event : logAppender.list) {
            assertThat(event.getFormattedMessage()).doesNotContain("jdoe@example.com");
        }
    }
}
