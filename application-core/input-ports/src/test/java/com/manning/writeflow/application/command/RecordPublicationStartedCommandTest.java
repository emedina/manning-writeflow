package com.manning.writeflow.application.command;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link RecordPublicationStartedCommand}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class RecordPublicationStartedCommandTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    @DisplayName("validateThenCreate should create a command when both identifiers are valid")
    void validateThenCreateShouldCreateCommandWhenIdentifiersAreValid() {
        //given
        //when
        final Either<Error, RecordPublicationStartedCommand> result =
            RecordPublicationStartedCommand.validateThenCreate(ARTICLE_ID, AUTHOR_ID);

        //then
        then(result.isRight()).isTrue();
        final RecordPublicationStartedCommand command = result.get();
        then(command.articleId()).isEqualTo(ARTICLE_ID);
        then(command.authorId()).isEqualTo(AUTHOR_ID);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("validateThenCreate should return validation errors when an identifier is invalid")
    @MethodSource("invalidIdentifiers")
    void validateThenCreateShouldReturnValidationErrorsWhenIdentifierIsInvalid(final String scenario,
                                                                               final String articleId,
                                                                               final String authorId) {
        //given

        //when
        final Either<Error, RecordPublicationStartedCommand> result =
            RecordPublicationStartedCommand.validateThenCreate(articleId, authorId);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    private static Stream<Arguments> invalidIdentifiers() {
        return Stream.of(
            Arguments.of("missing article id", null, AUTHOR_ID),
            Arguments.of("empty article id", "", AUTHOR_ID),
            Arguments.of("malformed article id", "bad", AUTHOR_ID),
            Arguments.of("missing author id", ARTICLE_ID, null),
            Arguments.of("empty author id", ARTICLE_ID, ""),
            Arguments.of("malformed author id", ARTICLE_ID, "bad"));
    }

    @Test
    @DisplayName("validateThenCreate should accumulate errors when both identifiers are invalid")
    void validateThenCreateShouldAccumulateErrorsWhenBothIdentifiersAreInvalid() {
        //given

        //when
        final Either<Error, RecordPublicationStartedCommand> result =
            RecordPublicationStartedCommand.validateThenCreate(null, "bad");

        //then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(2);
    }

}
