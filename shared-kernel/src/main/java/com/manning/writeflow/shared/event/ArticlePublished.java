package com.manning.writeflow.shared.event;

import java.time.Instant;

/**
 * Domain event raised when an article transitions to the published state.
 *
 * <p>As a cross-boundary contract this event is expressed with primitives only &mdash; the raw
 * article and author identifiers as strings plus the publication instant &mdash; rather than
 * with domain value objects. That keeps every consumer (output ports, listeners, the
 * integration-event publisher) free of any dependency on the domain module, which is the
 * concrete fix for the previous leak where the event referenced {@code ArticleId}/{@code AuthorId}.
 * The aggregate that raises the event still owns the decision to raise it; it simply hands over
 * primitive identifiers when it does.</p>
 *
 * @param articleId   the identifier of the published article
 * @param authorId    the identifier of the author who published the article
 * @param publishedAt the instant at which the article was published
 * @author Enrique Medina Montenegro
 */
public record ArticlePublished(String articleId, String authorId, Instant publishedAt) implements DomainEvent {
}
