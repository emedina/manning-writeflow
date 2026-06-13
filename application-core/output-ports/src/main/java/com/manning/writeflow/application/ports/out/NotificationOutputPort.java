package com.manning.writeflow.application.ports.out;

import com.emedina.sharedkernel.application.annotation.OutputPort;
import com.manning.writeflow.shared.dto.NotificationTextDTO;
import com.manning.writeflow.shared.dto.NotificationTypeDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * This port abstracts away the delivery of notifications to the subscribers of an author,
 * decoupling the application core from the concrete notification channel (e.g. email, push)
 * provided by the infrastructure.
 *
 * @author Enrique Medina Montenegro
 */
@OutputPort
public interface NotificationOutputPort {

    /**
     * Notifies the relevant subscribers with the given text and notification type.
     *
     * @param text the notification text to deliver
     * @param type the type of notification being delivered
     * @return {@link Either} the resulting {@link Error} on the left, or {@code Void} on success
     */
    Either<Error, Void> notify(final NotificationTextDTO text, final NotificationTypeDTO type);

}
