package com.manning.writeflow.application.ports.out;

import java.util.List;

import com.emedina.sharedkernel.application.annotation.OutputPort;
import com.manning.writeflow.shared.event.DomainEvent;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * This port abstracts away the publication of domain events raised by aggregates within the
 * application's own boundary, decoupling the application core from the concrete event-dispatch
 * mechanism provided by the infrastructure (e.g. Spring's {@code ApplicationEventPublisher}).
 *
 * @author Enrique Medina Montenegro
 */
@OutputPort
public interface DomainEventPublisherOutputPort {

    /**
     * Publishes the given list of domain events so that interested listeners can react to them.
     *
     * @param events the domain events to publish
     * @return either success, or an error if the event could not be published
     */
    Either<Error, Void> publish(final List<DomainEvent> events);

}
