package com.manning.writeflow.events;

import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.emedina.sharedkernel.command.Command;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.manning.writeflow.application.command.RecordPublicationStartedCommand;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordPublicationStartedListener}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class RecordPublicationStartedListenerTest {

    @Mock
    private CommandBus commandBus;

    @Captor
    private ArgumentCaptor<Command> commandCaptor;

    @InjectMocks
    private RecordPublicationStartedListener listener;

    @Test
    @DisplayName("on should dispatch a recording command for a publication request")
    void onShouldDispatchRecordingCommandForPublicationRequest() {
        //given
        final PublicationStarted event = this.publicationStarted();
        given(this.commandBus.execute(any(RecordPublicationStartedCommand.class))).willReturn(Either.right(null));

        //when
        this.listener.on(event);

        //then
        verify(this.commandBus).execute(this.commandCaptor.capture());
        then(this.commandCaptor.getValue()).isInstanceOf(RecordPublicationStartedCommand.class);
    }

    @Test
    @DisplayName("on should throw when recording a publication request fails")
    void onShouldThrowWhenRecordingPublicationRequestFails() {
        //given
        final PublicationStarted event = this.publicationStarted();
        final Error error = new Error.TechnicalError.PersistenceError("outbox", "publish", "db down");
        given(this.commandBus.execute(any(RecordPublicationStartedCommand.class))).willReturn(Either.left(error));

        //when
        final Throwable thrown = catchThrowable(() -> this.listener.on(event));

        //then
        then(thrown).isInstanceOf(IllegalStateException.class);
        verify(this.commandBus).execute(any(RecordPublicationStartedCommand.class));
    }

    private PublicationStarted publicationStarted() {
        return new PublicationStarted(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

}
