package com.manning.writeflow.application.command;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Instant;

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
class PublishArticleCommandTest {

    private static final String VALID_ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String VALID_AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void shouldCreateValidCommand_whenAllValidFieldsProvided() {
        // given
        final Instant publicationDate = Instant.parse("2026-06-07T10:15:30.00Z");

        // when
        final Either<Error, PublishArticleCommand> result =
            PublishArticleCommand.validateThenCreate(VALID_ARTICLE_ID, VALID_AUTHOR_ID, publicationDate);

        // then
        then(result.isRight()).isTrue();
        final PublishArticleCommand command = result.get();
        then(command.articleId()).isEqualTo(VALID_ARTICLE_ID);
        then(command.authorId()).isEqualTo(VALID_AUTHOR_ID);
        then(command.publicationDate()).isEqualTo(publicationDate);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL                                 , 22222222-2222-2222-2222-222222222222",
        "''                                   , 22222222-2222-2222-2222-222222222222",
        "not-a-uuid                           , 22222222-2222-2222-2222-222222222222",
        "11111111-1111-1111-1111-111111111111 , NULL",
        "11111111-1111-1111-1111-111111111111 , ''",
        "11111111-1111-1111-1111-111111111111 , not-a-uuid"
    }, nullValues = "NULL")
    void shouldReturnValidationErrors_whenAnIdentifierIsInvalid(final String articleId, final String authorId) {
        // given
        final Instant publicationDate = Instant.now();

        // when
        final Either<Error, PublishArticleCommand> result =
            PublishArticleCommand.validateThenCreate(articleId, authorId, publicationDate);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldReturnValidationError_whenPublicationDateIsNull() {
        // when
        final Either<Error, PublishArticleCommand> result =
            PublishArticleCommand.validateThenCreate(VALID_ARTICLE_ID, VALID_AUTHOR_ID, null);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldAccumulateAllValidationErrors_whenEveryFieldIsInvalid() {
        // when
        final Either<Error, PublishArticleCommand> result =
            PublishArticleCommand.validateThenCreate(null, "bad", null);

        // then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(3);
    }

}
