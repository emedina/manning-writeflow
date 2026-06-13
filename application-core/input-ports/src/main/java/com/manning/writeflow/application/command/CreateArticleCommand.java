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
 * Command to encapsulate a request to create a new draft article.
 *
 * <p>WriteFlow is Either-only: commands carry primitives only. The raw identifiers are validated as
 * syntactically valid UUIDs, the title as mandatory and bounded in length and the content as mandatory
 * at the boundary, but the validated values are stored and exposed as raw {@link String} so that no
 * domain type leaks into the input-ports layer; the handler re-parses them into the domain entity.</p>
 *
 * @author Enrique Medina Montenegro
 * @see Command
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateArticleCommand implements Command {

    private static final int MAX_TITLE_LENGTH = 500;

    private final String articleId;
    private final String authorId;
    private final String title;
    private final String content;

    /**
     * Validates the raw request data and creates a {@link CreateArticleCommand} if everything is valid.
     *
     * @param articleId the raw article identifier
     * @param authorId the raw identifier of the authoring author
     * @param title the title of the article
     * @param content the content of the article
     * @return {@link Either#right} with the command when valid, otherwise {@link Either#left} with the accumulated errors
     */
    public static Either<Error, CreateArticleCommand> validateThenCreate(final String articleId, final String authorId,
                                                                         final String title, final String content) {
        final List<ValidationError> errors = new ArrayList<>();

        Validations.validUuid(articleId, "articleId").peekLeft(errors::add);
        Validations.validUuid(authorId, "authorId").peekLeft(errors::add);
        Validations.mandatoryText(title, "title")
            .flatMap(value -> Validations.maxLength(value, MAX_TITLE_LENGTH, "title"))
            .peekLeft(errors::add);
        Validations.mandatoryText(content, "content").peekLeft(errors::add);

        if (!errors.isEmpty()) {
            return Either.left(new Error.ValidationErrors(List.copyOf(errors)));
        }

        return Either.right(new CreateArticleCommand(articleId, authorId, title, content));
    }

}
