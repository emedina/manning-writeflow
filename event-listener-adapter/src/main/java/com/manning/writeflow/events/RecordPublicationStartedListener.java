package com.manning.writeflow.events;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.manning.writeflow.application.command.RecordPublicationStartedCommand;
import com.manning.writeflow.shared.event.PublicationStarted;

import lombok.RequiredArgsConstructor;

/**
 * Records publication requests before commit so the Article state and outbox row commit together.
 * This listener remains public and non-final as required for a {@link TransactionalEventListener} adapter.
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see CommandBus
 */
@Adapter
@RequiredArgsConstructor
public class RecordPublicationStartedListener {

    private final CommandBus commandBus;

    /**
     * Dispatches the request-recording command before the surrounding transaction commits.
     *
     * @param event the event that publication was requested
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(final PublicationStarted event) {
        RecordPublicationStartedCommand.validateThenCreate(event.articleId(), event.authorId())
            .flatMap(this.commandBus::execute)
            .peekLeft(error -> {
                throw new IllegalStateException("Failed to record publication request for article ["
                    + event.articleId() + "]: " + error);
            });
    }

}
