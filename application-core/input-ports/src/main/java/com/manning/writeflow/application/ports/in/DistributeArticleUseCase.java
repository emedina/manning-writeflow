package com.manning.writeflow.application.ports.in;

import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;
import com.manning.writeflow.application.command.DistributeArticleCommand;
import com.manning.writeflow.shared.error.Error;

/**
 * Performs the distributeArticle use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@UseCase
public interface DistributeArticleUseCase extends CommandHandler<Error, DistributeArticleCommand> {

}
