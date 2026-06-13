package com.manning.writeflow.application.command;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link NotifySubscribersCommand}.
 *
 * @author Enrique Medina Montenegro
 */
class NotifySubscribersCommandTest {

    private static final String VALID_ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String VALID_AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void shouldCreateValidCommand_whenAllValidFieldsProvided() {
        // when
        final Either<Error, NotifySubscribersCommand> result =
            NotifySubscribersCommand.validateThenCreate(VALID_ARTICLE_ID, VALID_AUTHOR_ID);

        // then
        then(result.isRight()).isTrue();
        final NotifySubscribersCommand command = result.get();
        then(command.articleId()).isEqualTo(VALID_ARTICLE_ID);
        then(command.authorId()).isEqualTo(VALID_AUTHOR_ID);
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
        // when
        final Either<Error, NotifySubscribersCommand> result =
            NotifySubscribersCommand.validateThenCreate(articleId, authorId);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldAccumulateAllValidationErrors_whenEveryFieldIsInvalid() {
        // when
        final Either<Error, NotifySubscribersCommand> result =
            NotifySubscribersCommand.validateThenCreate(null, "bad");

        // then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(2);
    }

}
