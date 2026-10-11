package com.manning.writeflow.repositories;

import java.time.Instant;

/**
 * Outbox contract recording that publication was requested, not that the article was published.
 *
 * @param eventId the unique identifier of this integration event
 * @param articleId the identifier of the article
 * @param createdAt the instant at which this integration event was created
 * @author Enrique Medina Montenegro (emedina)
 */
public record PublicationStartedIntegrationEvent(String eventId, String articleId, Instant createdAt) {
}
