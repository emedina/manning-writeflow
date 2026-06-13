package com.manning.writeflow.repositories;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data repository for {@link OutboxMessage} rows.
 *
 * <p>Used internally by {@link OutboxIntegrationEventPublisher} to persist integration events
 * to the transactional outbox.</p>
 *
 * @author Enrique Medina Montenegro
 */
public interface SpringOutboxRepository extends CrudRepository<OutboxMessage, String> {

}
