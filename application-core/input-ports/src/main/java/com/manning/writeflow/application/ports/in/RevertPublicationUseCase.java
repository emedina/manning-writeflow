package com.manning.writeflow.application.ports.in;

import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;
import com.manning.writeflow.application.command.RevertPublicationCommand;
import com.manning.writeflow.shared.error.Error;

/**
 * Performs the revertPublication use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@UseCase
public interface RevertPublicationUseCase extends CommandHandler<Error, RevertPublicationCommand> {

}
