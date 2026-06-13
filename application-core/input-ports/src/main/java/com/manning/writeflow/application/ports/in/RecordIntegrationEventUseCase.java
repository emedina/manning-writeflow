package com.manning.writeflow.application.ports.in;

import com.manning.writeflow.application.command.RecordIntegrationEventCommand;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;

/**
 * Use case to record an integration event for an article that has been published.
 *
 * @author Enrique Medina Montenegro
 * @see UseCase
 */
@UseCase
public interface RecordIntegrationEventUseCase extends CommandHandler<Error, RecordIntegrationEventCommand> {
}
