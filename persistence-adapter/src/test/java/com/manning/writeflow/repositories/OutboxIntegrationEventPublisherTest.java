package com.manning.writeflow.repositories;

import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.PublicationStarted;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

/**
 * Unit tests for {@link OutboxIntegrationEventPublisher}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxIntegrationEventPublisher Tests")
class OutboxIntegrationEventPublisherTest {

    private static final String ARTICLE_ID = UUID.randomUUID().toString();
    private static final String AUTHOR_ID = UUID.randomUUID().toString();
    private static final Instant PUBLISHED_AT = Instant.parse("2026-06-08T10:15:30.00Z");

    @Mock
    private SpringOutboxRepository springOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxIntegrationEventPublisher publisher;

    private ArticlePublished anEvent() {
        return new ArticlePublished(ARTICLE_ID, AUTHOR_ID, PUBLISHED_AT);
    }

    @Test
    @DisplayName("When publishing successfully, then it should write an outbox row and return right")
    void shouldWriteOutboxRowAndReturnRight() {
        // Given
        given(this.objectMapper.writeValueAsString(any())).willReturn("{\"json\":true}");

        // When
        final Either<Error, Void> result = this.publisher.publish(anEvent());

        // Then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(this.springOutboxRepository).save(captor.capture());
        final OutboxMessage saved = captor.getValue();
        then(saved.getEventId()).isNotBlank();
        then(saved.getEventType()).isEqualTo("ArticlePublished");
        then(saved.getPayload()).isEqualTo("{\"json\":true}");
        then(saved.getCreatedAt()).isNotNull();
        then(saved.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("When the payload translates the article id, then the serialized event carries it")
    void shouldTranslateArticleIdIntoIntegrationEvent() {
        // Given
        given(this.objectMapper.writeValueAsString(any())).willReturn("payload");

        // When
        this.publisher.publish(anEvent());

        // Then
        final ArgumentCaptor<ArticlePublishedIntegrationEvent> captor =
            ArgumentCaptor.forClass(ArticlePublishedIntegrationEvent.class);
        verify(this.objectMapper).writeValueAsString(captor.capture());
        final ArticlePublishedIntegrationEvent translated = captor.getValue();
        then(translated.articleId()).isEqualTo(ARTICLE_ID);
        then(translated.publishedAt()).isEqualTo(PUBLISHED_AT);
        then(translated.eventId()).isNotBlank();
        then(translated.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("When recording a publication request, then it should write its outbox contract")
    void shouldWritePublicationStartedOutboxRow() {
        //given
        final PublicationStarted event = new PublicationStarted(ARTICLE_ID, AUTHOR_ID);
        given(this.objectMapper.writeValueAsString(any())).willReturn("publication-started-payload");

        //when
        final Either<Error, Void> result = this.publisher.publish(event);

        //then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<PublicationStartedIntegrationEvent> eventCaptor =
            ArgumentCaptor.forClass(PublicationStartedIntegrationEvent.class);
        verify(this.objectMapper).writeValueAsString(eventCaptor.capture());
        final PublicationStartedIntegrationEvent translated = eventCaptor.getValue();
        then(translated.eventId()).isNotBlank();
        then(translated.articleId()).isEqualTo(ARTICLE_ID);
        then(translated.createdAt()).isNotNull();

        final ArgumentCaptor<OutboxMessage> messageCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(this.springOutboxRepository).save(messageCaptor.capture());
        final OutboxMessage saved = messageCaptor.getValue();
        then(saved.getEventId()).isEqualTo(translated.eventId());
        then(saved.getEventType()).isEqualTo("PublicationStarted");
        then(saved.getPayload()).isEqualTo("publication-started-payload");
        then(saved.getCreatedAt()).isEqualTo(translated.createdAt());
        then(saved.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("When serialization fails, then it should return a PersistenceError")
    void shouldReturnPersistenceErrorOnSerializationFailure() {
        // Given
        given(this.objectMapper.writeValueAsString(any()))
            .willThrow(new JacksonException("bad json") {
            });

        // When
        final Either<Error, Void> result = this.publisher.publish(anEvent());

        // Then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.TechnicalError.PersistenceError.class);
        final var error = (Error.TechnicalError.PersistenceError) result.getLeft();
        then(error.origin()).isEqualTo("OutboxIntegrationEventPublisher");
        then(error.operation()).isEqualTo("publish");
    }

    @Test
    @DisplayName("When the outbox save fails, then it should return a PersistenceError")
    void shouldReturnPersistenceErrorOnSaveFailure() {
        // Given
        given(this.objectMapper.writeValueAsString(any())).willReturn("payload");
        final DataAccessException ex = new DataAccessResourceFailureException("db down");
        given(this.springOutboxRepository.save(any(OutboxMessage.class))).willThrow(ex);

        // When
        final Either<Error, Void> result = this.publisher.publish(anEvent());

        // Then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.TechnicalError.PersistenceError.class);
        final var error = (Error.TechnicalError.PersistenceError) result.getLeft();
        then(error.message()).isEqualTo("db down");
    }

    @Test
    @DisplayName("publish should propagate an unexpected failure when writing the outbox")
    void publishShouldPropagateUnexpectedFailureWhenWritingOutbox() {
        //given
        given(this.objectMapper.writeValueAsString(any())).willReturn("payload");
        final IllegalArgumentException failure = new IllegalArgumentException("invalid outbox state");
        given(this.springOutboxRepository.save(any(OutboxMessage.class))).willThrow(failure);

        //when
        final Throwable thrown = catchThrowable(() -> this.publisher.publish(anEvent()));

        //then
        then(thrown).isInstanceOf(IllegalStateException.class);
        then(thrown.getCause()).isSameAs(failure);
    }

}
