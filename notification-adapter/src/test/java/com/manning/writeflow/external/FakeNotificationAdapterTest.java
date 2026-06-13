package com.manning.writeflow.external;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.manning.writeflow.shared.dto.NotificationTextDTO;
import com.manning.writeflow.shared.dto.NotificationTypeDTO;
import com.manning.writeflow.shared.error.Error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vavr.control.Either;

/**
 * Unit tests for {@link FakeNotificationAdapter}, exercising the fake against a real instance and
 * asserting both the {@link Either} result and the log path it follows.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("FakeNotificationAdapter Tests")
class FakeNotificationAdapterTest {

    private FakeNotificationAdapter adapter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        this.adapter = new FakeNotificationAdapter();

        this.logger = (Logger) LoggerFactory.getLogger(FakeNotificationAdapter.class);
        this.appender = new ListAppender<>();
        this.appender.start();
        this.logger.addAppender(this.appender);
    }

    @AfterEach
    void tearDown() {
        this.logger.detachAppender(this.appender);
    }

    @Test
    @DisplayName("When notifying, then it returns a right Void result")
    void shouldReturnRight_whenNotifying() {
        // Given
        final var text = new NotificationTextDTO("An article you follow has just been published");
        final var type = NotificationTypeDTO.ARTICLE_PUBLISHED;

        // When
        final Either<Error, Void> result = this.adapter.notify(text, type);

        // Then
        then(result.isRight()).isTrue();
        then(result.get()).isNull();
    }

    @Test
    @DisplayName("When notifying, then it logs the pretend-email message at INFO level")
    void shouldLogPretendEmail_whenNotifying() {
        // Given
        final var text = new NotificationTextDTO("An article you follow has just been published");
        final var type = NotificationTypeDTO.ARTICLE_PUBLISHED;

        // When
        this.adapter.notify(text, type);

        // Then
        then(this.appender.list).hasSize(1);
        final ILoggingEvent event = this.appender.list.getFirst();
        then(event.getLevel()).isEqualTo(Level.INFO);
        then(event.getFormattedMessage())
            .isEqualTo("Pretend email sent: ARTICLE_PUBLISHED / An article you follow has just been published");
    }

    @Test
    @DisplayName("When notifying repeatedly, then it consistently returns right without throwing")
    void shouldReturnRightConsistently_whenNotifyingRepeatedly() {
        // Given
        final var text = new NotificationTextDTO("Repeat delivery");
        final var type = NotificationTypeDTO.ARTICLE_PUBLISHED;

        // When
        final Either<Error, Void> first = this.adapter.notify(text, type);
        final Either<Error, Void> second = this.adapter.notify(text, type);

        // Then
        then(first.isRight()).isTrue();
        then(second.isRight()).isTrue();
        then(this.appender.list).hasSize(2);
    }

}
