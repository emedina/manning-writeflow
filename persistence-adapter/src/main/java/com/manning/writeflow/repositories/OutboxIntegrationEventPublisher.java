package com.manning.writeflow.repositories;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataAccessException;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.IntegrationEventPublisherOutputPort;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter implementing {@link IntegrationEventPublisherOutputPort} using the transactional-outbox pattern.
 *
 * <p>It translates the {@link ArticlePublished} and {@link PublicationStarted} domain events into
 * their integration-event records, serializes them to JSON via the injected {@link ObjectMapper}, and persists them as
 * {@link OutboxMessage} rows within the current transaction. A separate relay process dispatches unpublished rows to
 * external consumers.</p>
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@Adapter
@RequiredArgsConstructor
class OutboxIntegrationEventPublisher implements IntegrationEventPublisherOutputPort {

    private static final String ORIGIN = "OutboxIntegrationEventPublisher";
    private static final String EVENT_TYPE = "ArticlePublished";
    private static final String PUBLICATION_STARTED_EVENT_TYPE = "PublicationStarted";

    private final SpringOutboxRepository springOutboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Translates the supplied domain event into an integration event and writes it to the transactional outbox.
     *
     * @param event the {@link ArticlePublished} domain event to relay
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left} with a
     * {@link Error.TechnicalError.PersistenceError}
     */
    @Override
    public Either<Error, Void> publish(final ArticlePublished event) {
        final Instant now = Instant.now();
        final ArticlePublishedIntegrationEvent integrationEvent = new ArticlePublishedIntegrationEvent(
            UUID.randomUUID().toString(),
            event.articleId(),
            event.publishedAt(),
            now
        );

        return Try.run(() -> {
                final String payload = this.objectMapper.writeValueAsString(integrationEvent);
                final OutboxMessage message = new OutboxMessage(
                    integrationEvent.eventId(),
                    EVENT_TYPE,
                    payload,
                    integrationEvent.createdAt(),
                    null
                );
                this.springOutboxRepository.save(message);
            })
            .toEither()
            .mapLeft(this::mapFailure);
    }

    /**
     * Translates the publication request into an integration event and writes it to the outbox.
     *
     * @param event the {@link PublicationStarted} domain event to relay
     * @return right on success, otherwise a persistence error
     */
    @Override
    public Either<Error, Void> publish(final PublicationStarted event) {
        final Instant now = Instant.now();
        final PublicationStartedIntegrationEvent integrationEvent = new PublicationStartedIntegrationEvent(
            UUID.randomUUID().toString(),
            event.articleId(),
            now
        );

        return Try.run(() -> {
                final String payload = this.objectMapper.writeValueAsString(integrationEvent);
                final OutboxMessage message = new OutboxMessage(
                    integrationEvent.eventId(),
                    PUBLICATION_STARTED_EVENT_TYPE,
                    payload,
                    integrationEvent.createdAt(),
                    null
                );
                this.springOutboxRepository.save(message);
            })
            .toEither()
            .mapLeft(this::mapFailure);
    }

    private Error mapFailure(final Throwable failure) {
        if (failure instanceof JacksonException || failure instanceof DataAccessException) {
            return new Error.TechnicalError.PersistenceError(ORIGIN, "publish", failure.getMessage());
        }

        throw new IllegalStateException("Unexpected failure while writing an outbox event", failure);
    }

}
