package com.manning.writeflow.application.query;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import io.vavr.control.Either;

/**
 * Unit tests for {@link GetArticleByIdQuery}.
 *
 * @author Enrique Medina Montenegro
 */
class GetArticleByIdQueryTest {

    private static final String VALID_ARTICLE_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void shouldCreateValidQuery_whenValidArticleIdProvided() {
        // when
        final Either<Error, GetArticleByIdQuery> result = GetArticleByIdQuery.validateThenCreate(VALID_ARTICLE_ID);

        // then
        then(result.isRight()).isTrue();
        then(result.get().articleId()).isEqualTo(VALID_ARTICLE_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not-a-uuid", "12345"})
    void shouldReturnValidationError_whenArticleIdIsInvalid(final String articleId) {
        // when
        final Either<Error, GetArticleByIdQuery> result = GetArticleByIdQuery.validateThenCreate(articleId);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
        then(errors.errors().get(0)).isInstanceOf(ValidationError.Invalid.class);
    }

}
