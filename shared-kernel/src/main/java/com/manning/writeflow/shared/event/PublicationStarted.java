package com.manning.writeflow.shared.event;

/**
 * Records that publication was requested for an article.
 *
 * @param articleId the article identifier
 * @param authorId  the author identifier
 * @author Enrique Medina Montenegro
 */
public record PublicationStarted(String articleId, String authorId) implements DomainEvent {
}
