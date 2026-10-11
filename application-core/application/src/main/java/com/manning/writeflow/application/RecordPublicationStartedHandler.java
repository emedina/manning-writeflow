package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.RecordPublicationStartedCommand;
import com.manning.writeflow.application.ports.in.RecordPublicationStartedUseCase;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Records a publication request through the integration-event output port.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ApplicationService
@RequiredArgsConstructor
class RecordPublicationStartedHandler implements RecordPublicationStartedUseCase {

    private final IntegrationEventPublisherOutputPort integrationEventPublisher;

    /**
     * Handles the command in the transaction that starts publication.
     *
     * @param command command to handle
     * @return right with {@code null} on success, otherwise the recording error
     */
    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final RecordPublicationStartedCommand command) {
        final PublicationStarted event = new PublicationStarted(command.articleId(), command.authorId());

        return this.integrationEventPublisher.publish(event);
    }

}
