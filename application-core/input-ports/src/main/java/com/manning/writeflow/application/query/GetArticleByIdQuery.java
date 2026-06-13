package com.manning.writeflow.application.query;

import java.util.List;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.Validations;
import com.emedina.sharedkernel.query.Query;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Query to encapsulate a request to retrieve an article by its identifier.
 *
 * <p>WriteFlow is Either-only: the raw identifier is validated as a syntactically valid UUID
 * at the boundary and exposed as a string so that the handler can parse it into the typed
 * value object.</p>
 *
 * @author Enrique Medina Montenegro
 * @see Query
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetArticleByIdQuery implements Query {

    private final String articleId;

    /**
     * Validates the raw request data and creates a {@link GetArticleByIdQuery} if it is valid.
     *
     * @param articleId the raw article identifier
     * @return {@link Either#right} with the query when valid, otherwise {@link Either#left} with the accumulated errors
     */
    public static Either<Error, GetArticleByIdQuery> validateThenCreate(final String articleId) {
        return Validations.validUuid(articleId, "articleId")
            .map(GetArticleByIdQuery::new)
            .mapLeft(error -> new Error.ValidationErrors(List.of(error)));
    }

}
