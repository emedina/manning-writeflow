package com.manning.writeflow.application.ports.out;

import com.emedina.sharedkernel.application.annotation.OutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;

/**
 * This port abstracts away the publication of integration events towards the outside world.
 * It receives a raised {@link ArticlePublished} or {@link PublicationStarted} domain event and
 * delegates to the adapter, which translates it into an integration event and persists it through
 * the transactional outbox.
 *
 * @author Enrique Medina Montenegro (emedina)
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

    /**
     * Records a publication request as an integration event.
     *
     * @param event the {@link PublicationStarted} domain event to relay
     * @return the resulting error on the left, or {@code Void} on success
     */
    Either<Error, Void> publish(final PublicationStarted event);

}
