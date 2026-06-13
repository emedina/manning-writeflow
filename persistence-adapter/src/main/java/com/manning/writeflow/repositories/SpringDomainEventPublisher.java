package com.manning.writeflow.repositories;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.shared.event.DomainEvent;

import lombok.RequiredArgsConstructor;

/**
 * Adapter implementing {@link DomainEventPublisherOutputPort} on top of Spring's
 * {@link ApplicationEventPublisher}.
 *
 * <p>Each drained domain event is republished as a Spring application event so that
 * transactional event listeners in other adapters can react to it within the appropriate
 * transaction phase.</p>
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
     */
    @Override
    public void publish(final List<DomainEvent> events) {
        events.forEach(this.applicationEventPublisher::publishEvent);
    }

}
