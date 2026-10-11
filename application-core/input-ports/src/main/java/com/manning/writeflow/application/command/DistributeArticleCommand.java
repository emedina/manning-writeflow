package com.manning.writeflow.application.command;

import java.util.ArrayList;
import java.util.List;

import com.emedina.sharedkernel.command.Command;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;
import com.manning.writeflow.shared.validation.Validations;
import io.vavr.control.Either;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Command to distribute an article.
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see Command
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistributeArticleCommand implements Command {

    private final String articleId;

    /**
     * Validates the command values and creates the command when they are valid.
     *
     * @param articleId the article identifier
     * @return the command or its validation errors
     */
    public static Either<Error, DistributeArticleCommand> validateThenCreate(final String articleId) {
        final List<ValidationError> errors = new ArrayList<>();
        Validations.validUuid(articleId, "articleId").peekLeft(errors::add);

        if (!errors.isEmpty()) {
            return Either.left(new Error.ValidationErrors(List.copyOf(errors)));
        }

        return Either.right(new DistributeArticleCommand(articleId));
    }

}
