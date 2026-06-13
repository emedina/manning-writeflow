package com.manning.writeflow.domain.entities;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import io.vavr.control.Either;

/**
 * Unit tests for {@link Title}.
 *
 * @author Enrique Medina Montenegro
 */
class TitleTest {

    @Test
    void shouldCreateTitle_whenValidTextProvided() {
        // given
        final String validTitle = "A Reasonable Article Title";

        // when
        final Either<Error, Title> result = Title.validateThenCreate(validTitle);

        // then
        then(result.isRight()).isTrue();
        then(result.get().value()).isEqualTo(validTitle);
    }

    @Test
    void shouldCreateTitle_whenExactlyMaxLength() {
        // given
        final String boundaryTitle = "t".repeat(500);

        // when
        final Either<Error, Title> result = Title.validateThenCreate(boundaryTitle);

        // then
        then(result.isRight()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldReturnValidationErrors_whenBlankTitleProvided(final String blankTitle) {
        // when
        final Either<Error, Title> result = Title.validateThenCreate(blankTitle);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).anyMatch(e -> e instanceof ValidationError.Mandatory);
    }

    @Test
    void shouldReturnValidationErrors_whenTitleExceedsMaxLength() {
        // given
        final String tooLong = "t".repeat(501);

        // when
        final Either<Error, Title> result = Title.validateThenCreate(tooLong);

        // then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
        then(errors.errors().get(0)).isInstanceOf(ValidationError.MustHaveValue.class);
    }

}
