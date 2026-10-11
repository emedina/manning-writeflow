package com.manning.writeflow.events;

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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.command.Command;
import com.emedina.sharedkernel.command.core.CommandBus;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordIntegrationEventListener}.
 *
 * @author Enrique Medina Montenegro (emedina)
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
    @DisplayName("on should dispatch a recording command when an article is published")
    void onShouldDispatchRecordingCommandWhenArticleIsPublished() {
        //given
        final ArticlePublished event = anArticlePublishedEvent();
        given(this.commandBus.execute(any(RecordIntegrationEventCommand.class))).willReturn(Either.right(null));

        //when
        this.listener.on(event);

        //then
        verify(this.commandBus).execute(this.commandCaptor.capture());
        then(this.commandCaptor.getValue()).isInstanceOf(RecordIntegrationEventCommand.class);
    }

    @Test
    @DisplayName("on should throw when recording an article publication fails")
    void onShouldThrowWhenRecordingArticlePublicationFails() {
        //given
        final ArticlePublished event = anArticlePublishedEvent();
        final Error error = new Error.TechnicalError.PersistenceError("outbox", "publish", "db down");
        given(this.commandBus.execute(any(RecordIntegrationEventCommand.class))).willReturn(Either.left(error));

        //when
        final Throwable thrown = catchThrowable(() -> this.listener.on(event));

        //then
        then(thrown).isInstanceOf(IllegalStateException.class);
        verify(this.commandBus).execute(any(RecordIntegrationEventCommand.class));
    }

    private static ArticlePublished anArticlePublishedEvent() {
        final String articleId = UUID.randomUUID().toString();
        final String authorId = UUID.randomUUID().toString();
        return new ArticlePublished(articleId, authorId, Instant.now());
    }

}
