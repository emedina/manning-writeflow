package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.NotifySubscribersCommand;
import com.manning.writeflow.application.ports.in.NotifySubscribersUseCase;
import com.manning.writeflow.application.ports.out.NotificationOutputPort;
import com.manning.writeflow.shared.dto.NotificationTextDTO;
import com.manning.writeflow.shared.dto.NotificationTypeDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Orchestration logic for the use case to notify the subscribers of an author about a
 * published article.
 *
 * <p>The handler builds the notification payload and delegates delivery to the
 * {@link NotificationOutputPort}, keeping the application core agnostic of the concrete
 * notification channel.</p>
 *
 * @author Enrique Medina Montenegro
 * @see ApplicationService
 */
@ApplicationService
@RequiredArgsConstructor
class NotifySubscribersHandler implements NotifySubscribersUseCase {

    private final NotificationOutputPort notificationOutputPort;

    /**
     * Handles the command.
     *
     * @param command command to handle
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left} with the error
     */
    @Override
    @Transactional
    public Either<Error, Void> handle(final NotifySubscribersCommand command) {
        return this.notificationOutputPort.notify(
            new NotificationTextDTO("An article you follow has just been published"),
            NotificationTypeDTO.ARTICLE_PUBLISHED);
    }

}
