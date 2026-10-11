package com.manning.writeflow.events;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.shared.event.ArticlePublished;

import lombok.RequiredArgsConstructor;

/**
 * Event-listener adapter that reacts to the {@link ArticlePublished} domain event by recording an
 * integration event for the published article.
 *
 * <p>The listener fires in the {@link TransactionPhase#BEFORE_COMMIT} phase so that the integration
 * event (e.g. an outbox record) is written within the same transaction that published the article,
 * thus preserving atomicity between the business change and the integration event.</p>
 *
 * <p>The raw identifiers carried by the domain event are turned into a
 * {@link RecordIntegrationEventCommand} via its Either-returning factory, and the command is then
 * dispatched through the {@link CommandBus}. A failure is thrown so the surrounding transaction
 * cannot commit without its outbox row.</p>
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see CommandBus
 * @see ArticlePublished
 */
@Adapter
@RequiredArgsConstructor
public class RecordIntegrationEventListener {

    private final CommandBus commandBus;

    /**
     * Handles the {@link ArticlePublished} domain event before the surrounding transaction commits.
     *
     * @param event the domain event signaling that an article has been published
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(final ArticlePublished event) {
        RecordIntegrationEventCommand.validateThenCreate(
                event.articleId(),
                event.authorId(),
                event.publishedAt())
            .flatMap(this.commandBus::execute)
            .peekLeft(error -> {
                throw new IllegalStateException("Failed to record integration event for article ["
                    + event.articleId() + "]: " + error);
            });
    }

}
