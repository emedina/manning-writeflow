package com.manning.writeflow.application;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.RecordPublicationStartedCommand;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordPublicationStartedHandler}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class RecordPublicationStartedHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private IntegrationEventPublisherOutputPort integrationEventPublisher;

    @Mock
    private RecordPublicationStartedCommand command;

    @InjectMocks
    private RecordPublicationStartedHandler handler;

    @Test
    @DisplayName("handle should publish the reconstructed publication request")
    void handleShouldPublishReconstructedPublicationRequest() {
        //given
        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.integrationEventPublisher.publish(any(PublicationStarted.class))).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<PublicationStarted> captor = ArgumentCaptor.forClass(PublicationStarted.class);
        verify(this.integrationEventPublisher).publish(captor.capture());
        then(captor.getValue().articleId()).isEqualTo(ARTICLE_ID);
        then(captor.getValue().authorId()).isEqualTo(AUTHOR_ID);
    }

}
