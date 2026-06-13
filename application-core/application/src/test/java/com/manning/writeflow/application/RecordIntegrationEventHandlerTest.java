package com.manning.writeflow.application;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordIntegrationEventHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class RecordIntegrationEventHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private IntegrationEventPublisherOutputPort integrationEventPublisher;

    @Mock
    private RecordIntegrationEventCommand command;

    @InjectMocks
    private RecordIntegrationEventHandler handler;

    @Test
    void shouldPublishReconstructedEvent_whenCommandIsValid() {
        // given
        final Instant publishedAt = Instant.parse("2026-06-07T10:15:30.00Z");

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publishedAt()).willReturn(publishedAt);
        given(this.integrationEventPublisher.publish(org.mockito.ArgumentMatchers.any(ArticlePublished.class)))
            .willReturn(Either.right(null));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<ArticlePublished> captor = ArgumentCaptor.forClass(ArticlePublished.class);
        verify(this.integrationEventPublisher).publish(captor.capture());
        final ArticlePublished published = captor.getValue();
        then(published.articleId()).isEqualTo(ARTICLE_ID);
        then(published.authorId()).isEqualTo(AUTHOR_ID);
        then(published.publishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void shouldReturnPersistenceError_whenPublicationFails() {
        // given
        final Instant publishedAt = Instant.parse("2026-06-07T10:15:30.00Z");
        final Error.TechnicalError.PersistenceError persistenceError =
            new Error.TechnicalError.PersistenceError("OutboxIntegrationEventPublisher", "publish", "boom");

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publishedAt()).willReturn(publishedAt);
        given(this.integrationEventPublisher.publish(org.mockito.ArgumentMatchers.any(ArticlePublished.class)))
            .willReturn(Either.left(persistenceError));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(persistenceError);
    }

}
