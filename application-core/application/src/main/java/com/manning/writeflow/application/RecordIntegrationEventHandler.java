package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.application.ports.in.RecordIntegrationEventUseCase;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Orchestration logic for the use case to record an integration event for a published article.
 *
 * <p>The command carries primitives only and the {@link ArticlePublished} event is itself a
 * primitive-typed shared contract, so the handler simply reconstructs the event from the command
 * fields and hands it to the integration event publisher, which is responsible for translating it
 * into an integration event and writing it to the transactional outbox.</p>
 *
 * @author Enrique Medina Montenegro
 * @see ApplicationService
 */
@ApplicationService
@RequiredArgsConstructor
class RecordIntegrationEventHandler implements RecordIntegrationEventUseCase {

    private final IntegrationEventPublisherOutputPort integrationEventPublisher;

    /**
     * Handles the command.
     *
     * @param command command to handle
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left} with the error
     */
    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final RecordIntegrationEventCommand command) {
        final ArticlePublished event = new ArticlePublished(
            command.articleId(), command.authorId(), command.publishedAt());

        return this.integrationEventPublisher.publish(event);
    }

}
