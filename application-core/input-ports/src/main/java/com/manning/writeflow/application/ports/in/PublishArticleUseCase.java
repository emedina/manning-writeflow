package com.manning.writeflow.application.ports.in;

import com.manning.writeflow.application.command.PublishArticleCommand;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.command.core.CommandHandler;

/**
 * Use case to publish an existing draft article.
 *
 * @author Enrique Medina Montenegro
 * @see UseCase
 */
@UseCase
public interface PublishArticleUseCase extends CommandHandler<Error, PublishArticleCommand> {
}
