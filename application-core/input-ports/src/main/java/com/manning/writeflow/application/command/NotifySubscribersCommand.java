package com.manning.writeflow.application.command;

import java.util.ArrayList;
import java.util.List;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;
import com.manning.writeflow.shared.validation.Validations;
import com.emedina.sharedkernel.command.Command;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Command to encapsulate a request to notify the subscribers of an author about a published article.
 *
 * <p>WriteFlow is Either-only: commands carry primitives only. The raw identifiers are validated as
 * syntactically valid UUIDs at the boundary, but the validated values are stored and exposed as raw
 * {@link String} so that no domain type leaks into the input-ports layer.</p>
 *
 * @author Enrique Medina Montenegro
 * @see Command
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotifySubscribersCommand implements Command {

    private final String articleId;
    private final String authorId;

    /**
     * Validates the raw request data and creates a {@link NotifySubscribersCommand} if everything is valid.
     *
     * @param articleId the raw article identifier
     * @param authorId the raw author identifier
     * @return {@link Either#right} with the command when valid, otherwise {@link Either#left} with the accumulated errors
     */
    public static Either<Error, NotifySubscribersCommand> validateThenCreate(final String articleId,
                                                                             final String authorId) {
        final List<ValidationError> errors = new ArrayList<>();

        Validations.validUuid(articleId, "articleId").peekLeft(errors::add);
        Validations.validUuid(authorId, "authorId").peekLeft(errors::add);

        if (!errors.isEmpty()) {
            return Either.left(new Error.ValidationErrors(List.copyOf(errors)));
        }

        return Either.right(new NotifySubscribersCommand(articleId, authorId));
    }

}
