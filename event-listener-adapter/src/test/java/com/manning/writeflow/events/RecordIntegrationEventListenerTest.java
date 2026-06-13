package com.manning.writeflow.events;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;
import com.emedina.sharedkernel.command.Command;
import com.emedina.sharedkernel.command.core.CommandBus;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordIntegrationEventListener}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class RecordIntegrationEventListenerTest {

    @Mock
    private CommandBus commandBus;

    @Captor
    private ArgumentCaptor<Command> commandCaptor;

    @InjectMocks
    private RecordIntegrationEventListener listener;

    @Test
    void should_dispatch_a_record_integration_event_command_when_an_article_is_published() {
        // given
        final ArticlePublished event = anArticlePublishedEvent();
        given(this.commandBus.execute(any(RecordIntegrationEventCommand.class))).willReturn(Either.right(null));

        // when
        this.listener.on(event);

        // then
        verify(this.commandBus).execute(this.commandCaptor.capture());
        then(this.commandCaptor.getValue()).isInstanceOf(RecordIntegrationEventCommand.class);
    }

    @Test
    void should_swallow_and_log_a_command_bus_failure_without_propagating() {
        // given
        final ArticlePublished event = anArticlePublishedEvent();
        final Error error = new Error.ValidationErrors(
            List.<ValidationError>of(new ValidationError.Mandatory("articleId")));
        given(this.commandBus.execute(any(RecordIntegrationEventCommand.class))).willReturn(Either.left(error));

        // when
        this.listener.on(event);

        // then
        verify(this.commandBus).execute(any(RecordIntegrationEventCommand.class));
    }

    private static ArticlePublished anArticlePublishedEvent() {
        final String articleId = UUID.randomUUID().toString();
        final String authorId = UUID.randomUUID().toString();
        return new ArticlePublished(articleId, authorId, Instant.now());
    }

}
