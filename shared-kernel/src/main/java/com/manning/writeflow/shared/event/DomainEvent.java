package com.manning.writeflow.shared.event;

import java.io.Serializable;

/**
 * Marker interface for domain events raised by aggregates in WriteFlow.
 *
 * <p>The event contract lives in the shared kernel rather than in the domain module so that
 * any layer that needs to publish or react to an event &mdash; output ports, event-listener
 * adapters, the integration-event publisher &mdash; can depend on it without depending on the
 * domain module. Keeping the contract here is what prevents the domain from leaking across the
 * hexagon's boundaries: events that travel between layers are expressed with primitives and
 * shared types only.</p>
 *
 * <p>The Architecture Kernel ({@code io.github.emedina:shared-kernel-domain:1.0.0}) does not
 * ship a generic {@code DomainEvent} marker, so WriteFlow defines its own here.</p>
 *
 * @author Enrique Medina Montenegro
 */
public interface DomainEvent extends Serializable {
}
