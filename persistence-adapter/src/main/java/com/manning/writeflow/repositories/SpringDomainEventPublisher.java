package com.manning.writeflow.repositories;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.DomainEvent;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * Adapter implementing {@link DomainEventPublisherOutputPort} on top of Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Each drained domain event is republished as a Spring application event so that
 * transactional event listeners in other adapters can react to it within the appropriate transaction phase.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Adapter
@RequiredArgsConstructor
class SpringDomainEventPublisher implements DomainEventPublisherOutputPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publishes each supplied domain event through Spring's application event mechanism.
     *
     * @param events the domain events to publish
     * @return either success, or an error if the event could not be published
     */
    @Override
    public Either<Error, Void> publish(final List<DomainEvent> events) {
        return Try.run(() -> events.forEach(this.applicationEventPublisher::publishEvent))
            .toEither()
            .mapLeft(t -> new Error.TechnicalError.SomethingWentWrong(
                "SpringDomainEventPublisher.publish()", t.getMessage()));
    }

}
