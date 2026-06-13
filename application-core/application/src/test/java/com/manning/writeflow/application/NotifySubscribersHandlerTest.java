package com.manning.writeflow.application;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.NotifySubscribersCommand;
import com.manning.writeflow.application.ports.out.NotificationOutputPort;
import com.manning.writeflow.shared.dto.NotificationTextDTO;
import com.manning.writeflow.shared.dto.NotificationTypeDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link NotifySubscribersHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class NotifySubscribersHandlerTest {

    @Mock
    private NotificationOutputPort notificationOutputPort;

    @Mock
    private NotifySubscribersCommand command;

    @InjectMocks
    private NotifySubscribersHandler handler;

    @Test
    void shouldNotifySubscribers_whenCommandIsValid() {
        // given
        given(this.notificationOutputPort.notify(
            org.mockito.ArgumentMatchers.any(NotificationTextDTO.class),
            org.mockito.ArgumentMatchers.eq(NotificationTypeDTO.ARTICLE_PUBLISHED)))
            .willReturn(Either.right(null));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<NotificationTextDTO> textCaptor = ArgumentCaptor.forClass(NotificationTextDTO.class);
        verify(this.notificationOutputPort).notify(textCaptor.capture(), org.mockito.ArgumentMatchers.eq(
            NotificationTypeDTO.ARTICLE_PUBLISHED));
        then(textCaptor.getValue().value()).isNotBlank();
    }

    @Test
    void shouldReturnError_whenNotificationDeliveryFails() {
        // given
        final Error.TechnicalError.SomethingWentWrong error =
            new Error.TechnicalError.SomethingWentWrong("FakeNotificationAdapter", "boom");

        given(this.notificationOutputPort.notify(
            org.mockito.ArgumentMatchers.any(NotificationTextDTO.class),
            org.mockito.ArgumentMatchers.eq(NotificationTypeDTO.ARTICLE_PUBLISHED)))
            .willReturn(Either.left(error));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(error);
    }

}
