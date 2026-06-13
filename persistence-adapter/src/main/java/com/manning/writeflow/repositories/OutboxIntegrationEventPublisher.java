package com.manning.writeflow.repositories;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataAccessException;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter implementing {@link IntegrationEventPublisherOutputPort} using the transactional-outbox
 * pattern.
 *
 * <p>It translates the {@link ArticlePublished} domain event into an
 * {@link ArticlePublishedIntegrationEvent}, serializes it to JSON via the injected
 * {@link ObjectMapper}, and persists it as an {@link OutboxMessage} row within the current
 * transaction. A separate relay process is responsible for actually dispatching unpublished
 * rows to external consumers.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Adapter
@RequiredArgsConstructor
class OutboxIntegrationEventPublisher implements IntegrationEventPublisherOutputPort {

    private static final String ORIGIN = "OutboxIntegrationEventPublisher";
    private static final String EVENT_TYPE = "ArticlePublished";

    private final SpringOutboxRepository springOutboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Translates the supplied domain event into an integration event and writes it to the
     * transactional outbox.
     *
     * @param event the {@link ArticlePublished} domain event to relay
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left}
     * with a {@link Error.TechnicalError.PersistenceError}
     */
    @Override
    public Either<Error, Void> publish(final ArticlePublished event) {
        final Instant now = Instant.now();
        final ArticlePublishedIntegrationEvent integrationEvent = new ArticlePublishedIntegrationEvent(
            UUID.randomUUID().toString(),
            event.articleId(),
            event.publishedAt(),
            now);

        try {
            final String payload = this.objectMapper.writeValueAsString(integrationEvent);
            final OutboxMessage message = new OutboxMessage(
                integrationEvent.eventId(),
                EVENT_TYPE,
                payload,
                integrationEvent.createdAt(),
                null);
            this.springOutboxRepository.save(message);

            return Either.right(null);
        } catch (final JacksonException e) {
            return Either.left(new Error.TechnicalError.PersistenceError(ORIGIN, "publish", e.getMessage()));
        } catch (final DataAccessException e) {
            return Either.left(new Error.TechnicalError.PersistenceError(ORIGIN, "publish", e.getMessage()));
        }
    }

}
