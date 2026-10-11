package com.manning.writeflow.application.command;

import java.time.Instant;
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
 * Command to confirm an article's Publication.
 *
 * @author Enrique Medina Montenegro (emedina)
 * @see Command
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfirmPublicationCommand implements Command {

    private final String articleId;
    private final Instant publishedAt;

    /**
     * Validates the command values and creates the command when they are valid.
     *
     * @param articleId the articleId
     * @param publishedAt the publishedAt
     * @return the command or its validation errors
     */
    public static Either<Error, ConfirmPublicationCommand> validateThenCreate(final String articleId,
        final Instant publishedAt) {
        final List<ValidationError> errors = new ArrayList<>();
        Validations.validUuid(articleId, "articleId").peekLeft(errors::add);
        Validations.mandatory(publishedAt, "publishedAt").peekLeft(errors::add);

        if (!errors.isEmpty()) {
            return Either.left(new Error.ValidationErrors(List.copyOf(errors)));
        }

        return Either.right(new ConfirmPublicationCommand(articleId, publishedAt));
    }

}
