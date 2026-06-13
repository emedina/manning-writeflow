package com.manning.writeflow.external;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.NotificationOutputPort;
import com.manning.writeflow.shared.dto.NotificationTextDTO;
import com.manning.writeflow.shared.dto.NotificationTypeDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;

/**
 * Fake stand-in implementation of {@link NotificationOutputPort} that does not deliver any real
 * notification: instead of contacting an external email gateway it simply logs the message it
 * would have sent and reports success.
 * <p>
 * This adapter exists purely so that the Ch9 worked example is runnable end-to-end without any
 * external infrastructure. The real {@code EmailNotificationAdapter}, which integrates with an
 * actual email provider, is introduced in Ch5.
 *
 * @author Enrique Medina Montenegro
 */
@Adapter
@Slf4j
class FakeNotificationAdapter implements NotificationOutputPort {

    /**
     * Pretends to deliver a notification by logging it and always reporting success.
     *
     * @param text the notification text that would have been delivered
     * @param type the type of notification being delivered
     * @return {@link Either} always {@code Either.right(null)}, since this fake never fails
     */
    @Override
    public Either<Error, Void> notify(final NotificationTextDTO text, final NotificationTypeDTO type) {
        log.info("Pretend email sent: {} / {}", type, text.value());
        return Either.right(null);
    }

}
