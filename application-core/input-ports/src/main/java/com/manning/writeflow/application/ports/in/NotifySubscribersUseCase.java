package com.manning.writeflow.application.ports.in;

import com.manning.writeflow.application.command.NotifySubscribersCommand;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;

/**
 * Use case to notify the subscribers of an author that a new article has been published.
 *
 * @author Enrique Medina Montenegro
 * @see UseCase
 */
@UseCase
public interface NotifySubscribersUseCase extends CommandHandler<Error, NotifySubscribersCommand> {
}
