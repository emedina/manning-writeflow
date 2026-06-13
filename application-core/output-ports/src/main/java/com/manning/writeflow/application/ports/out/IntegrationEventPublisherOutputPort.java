package com.manning.writeflow.application.ports.out;

import com.emedina.sharedkernel.application.annotation.OutputPort;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * This port abstracts away the publication of integration events towards the outside world.
 * It receives the raised {@link ArticlePublished} domain event and delegates to the adapter,
 * which is responsible for translating it into an integration event and persisting it through
 * the transactional outbox so it can be reliably relayed to external consumers.
 *
 * @author Enrique Medina Montenegro
 */
@OutputPort
public interface IntegrationEventPublisherOutputPort {

    /**
     * Publishes the given domain event as an integration event towards external consumers.
     *
     * @param event the {@link ArticlePublished} domain event to relay
     * @return {@link Either} the resulting {@link Error} on the left, or {@code Void} on success
     */
    Either<Error, Void> publish(final ArticlePublished event);

}
