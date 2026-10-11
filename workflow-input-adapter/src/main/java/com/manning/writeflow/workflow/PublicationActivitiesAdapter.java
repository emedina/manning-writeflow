package com.manning.writeflow.workflow;

import java.time.Instant;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.emedina.sharedkernel.command.Command;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.manning.writeflow.application.command.ConfirmPublicationCommand;
import com.manning.writeflow.application.command.DistributeArticleCommand;
import com.manning.writeflow.application.command.RevertPublicationCommand;
import com.manning.writeflow.application.command.WithdrawArticleCommand;
import com.manning.writeflow.shared.error.Error;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Dispatches Publication activities through the command bus.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@Adapter
@ActivityImpl(workers = "publication")
@RequiredArgsConstructor
final class PublicationActivitiesAdapter implements PublicationActivities {

    private static final String ERROR_TYPE = "WriteFlowApplicationError";

    private final CommandBus commandBus;

    @Override
    public void distributeArticle(final String articleId) {
        this.dispatch(DistributeArticleCommand.validateThenCreate(articleId));
    }

    @Override
    public void confirmPublication(final String articleId) {
        this.dispatch(ConfirmPublicationCommand.validateThenCreate(articleId, Instant.now()));
    }

    @Override
    public void revertPublication(final String articleId) {
        this.dispatch(RevertPublicationCommand.validateThenCreate(articleId));
    }

    @Override
    public void withdrawArticle(final String articleId) {
        this.dispatch(WithdrawArticleCommand.validateThenCreate(articleId));
    }

    private <C extends Command> void dispatch(final Either<Error, C> command) {
        final Either<Error, Void> result = command.flatMap(this.commandBus::execute);
        result.fold(
            error -> {
                throw this.toApplicationFailure(error);
            },
            _ -> null
        );
    }

    private RuntimeException toApplicationFailure(final Error error) {
        return switch (error) {
            case Error.ValidationErrors _, Error.MultipleErrors _, Error.BusinessError _ ->
                ApplicationFailure.newNonRetryableFailure(error.toString(), ERROR_TYPE);
            case Error.TechnicalError _ -> ApplicationFailure.newFailure(error.toString(), ERROR_TYPE);
        };
    }

}
