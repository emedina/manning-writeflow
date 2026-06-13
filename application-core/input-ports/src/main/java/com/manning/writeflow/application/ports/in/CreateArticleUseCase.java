package com.manning.writeflow.application.ports.in;

import com.manning.writeflow.application.command.CreateArticleCommand;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;

/**
 * Use case to create a new draft article.
 *
 * @author Enrique Medina Montenegro
 * @see UseCase
 */
@UseCase
public interface CreateArticleUseCase extends CommandHandler<Error, CreateArticleCommand> {
}
