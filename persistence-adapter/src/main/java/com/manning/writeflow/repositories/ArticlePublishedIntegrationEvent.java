package com.manning.writeflow.repositories;

import java.time.Instant;

/**
 * Integration-event representation of a published article, suitable for relaying to external
 * consumers.
 *
 * <p>Unlike the domain event {@code ArticlePublished}, this is a flattened, primitive-typed,
 * serialization-friendly record produced by {@link OutboxIntegrationEventPublisher} before the
 * event is written to the transactional outbox.</p>
 *
 * @param eventId     the unique identifier of this integration event
 * @param articleId   the identifier of the published article
 * @param publishedAt the instant at which the article was published
 * @param createdAt   the instant at which this integration event was created
 * @author Enrique Medina Montenegro
 */
public record ArticlePublishedIntegrationEvent(String eventId, String articleId, Instant publishedAt,
        Instant createdAt) {
}
