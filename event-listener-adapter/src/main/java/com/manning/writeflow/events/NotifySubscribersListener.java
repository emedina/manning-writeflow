package com.manning.writeflow.events;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.manning.writeflow.application.command.NotifySubscribersCommand;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.emedina.sharedkernel.application.annotation.Adapter;
import com.emedina.sharedkernel.command.core.CommandBus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event-listener adapter that reacts to the {@link ArticlePublished} domain event by notifying the
 * subscribers of the author about the newly published article.
 *
 * <p>The listener fires in the {@link TransactionPhase#AFTER_COMMIT} phase: notifications are an
 * external side effect that must only happen once the article has been durably persisted. Firing
 * after commit guarantees that subscribers are never told about an article that was ultimately
 * rolled back.</p>
 *
 * <p>The raw identifiers carried by the domain event are turned into a
 * {@link NotifySubscribersCommand} via its Either-returning factory, and the command is then
 * dispatched through the {@link CommandBus}. Any failure is logged rather than propagated.</p>
 *
 * @author Enrique Medina Montenegro
 * @see CommandBus
 * @see ArticlePublished
 */
@Slf4j
@Adapter
@RequiredArgsConstructor
public class NotifySubscribersListener {

    private final CommandBus commandBus;

    /**
     * Handles the {@link ArticlePublished} domain event after the surrounding transaction commits.
     *
     * @param event the domain event signalling that an article has been published
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(final ArticlePublished event) {
        NotifySubscribersCommand.validateThenCreate(
                event.articleId(),
                event.authorId())
            .flatMap(commandBus::execute)
            .peekLeft(error -> log.warn("Failed to notify subscribers for article [{}]: {}",
                event.articleId(), error));
    }

}
