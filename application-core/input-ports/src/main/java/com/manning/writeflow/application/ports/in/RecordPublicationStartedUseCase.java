package com.manning.writeflow.application.ports.in;

import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;
import com.manning.writeflow.application.command.RecordPublicationStartedCommand;
import com.manning.writeflow.shared.error.Error;

/**
 * Use case to record a publication request in the transactional outbox.
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see UseCase
 */
@UseCase
public interface RecordPublicationStartedUseCase
        extends CommandHandler<Error, RecordPublicationStartedCommand> {
}
