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
 * Command to record that an author requested publication of an article.
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see Command
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecordPublicationStartedCommand implements Command {

    private final String articleId;
    private final String authorId;

    /**
     * Validates the identifiers and creates the command when both are valid UUIDs.
     *
     * @param articleId the raw article identifier
     * @param authorId the raw author identifier
     * @return the command when valid, otherwise the accumulated validation errors
     */
    public static Either<Error, RecordPublicationStartedCommand> validateThenCreate(final String articleId,
                                                                                    final String authorId) {
        final List<ValidationError> errors = new ArrayList<>();

        Validations.validUuid(articleId, "articleId").peekLeft(errors::add);
        Validations.validUuid(authorId, "authorId").peekLeft(errors::add);

        if (!errors.isEmpty()) {
            return Either.left(new Error.ValidationErrors(List.copyOf(errors)));
        }

        return Either.right(new RecordPublicationStartedCommand(articleId, authorId));
    }

}
