package com.manning.writeflow.application.command;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link PublishArticleCommand}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("PublishArticleCommand tests")
class PublishArticleCommandTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    @DisplayName("When both identifiers are valid, then the command should preserve them")
    void validateThenCreateShouldPreserveIdentifiersWhenTheyAreValid() {
        //given
        final String articleId = ARTICLE_ID;
        final String authorId = AUTHOR_ID;

        //when
        final Either<Error, PublishArticleCommand> result =
                PublishArticleCommand.validateThenCreate(articleId, authorId);

        //then
        then(result.isRight()).isTrue();
        then(result.get().articleId()).isEqualTo(ARTICLE_ID);
        then(result.get().authorId()).isEqualTo(AUTHOR_ID);
    }

    @ParameterizedTest(name = "[{index}] articleId={0}, authorId={1}")
    @CsvSource(value = {
        "NULL                                 , 22222222-2222-2222-2222-222222222222",
        "''                                   , 22222222-2222-2222-2222-222222222222",
        "not-a-uuid                           , 22222222-2222-2222-2222-222222222222",
        "11111111-1111-1111-1111-111111111111 , NULL",
        "11111111-1111-1111-1111-111111111111 , ''",
        "11111111-1111-1111-1111-111111111111 , not-a-uuid"
    }, nullValues = "NULL")
    @DisplayName("When an identifier is invalid, then the command should return validation errors")
    void validateThenCreateShouldReturnValidationErrorsWhenAnIdentifierIsInvalid(
            final String articleId,
            final String authorId) {
        //given
        final String invalidArticleId = articleId;
        final String invalidAuthorId = authorId;

        //when
        final Either<Error, PublishArticleCommand> result =
                PublishArticleCommand.validateThenCreate(invalidArticleId, invalidAuthorId);

        //then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors validationErrors = (Error.ValidationErrors) result.getLeft();
        then(validationErrors.errors()).hasSize(1);
    }

    @Test
    @DisplayName("When both identifiers are invalid, then the command should accumulate both errors")
    void validateThenCreateShouldAccumulateErrorsWhenBothIdentifiersAreInvalid() {
        //given
        final String invalidArticleId = null;
        final String invalidAuthorId = "bad";

        //when
        final Either<Error, PublishArticleCommand> result =
                PublishArticleCommand.validateThenCreate(invalidArticleId, invalidAuthorId);

        //then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors validationErrors = (Error.ValidationErrors) result.getLeft();
        then(validationErrors.errors()).hasSize(2);
    }
}
