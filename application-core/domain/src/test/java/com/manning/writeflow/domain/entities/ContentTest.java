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
 * Unit tests for {@link Content}.
 *
 * @author Enrique Medina Montenegro
 */
class ContentTest {

    @Test
    void shouldCreateContent_whenValidTextProvided() {
        // given
        final String validContent = "The body of the article goes here.";

        // when
        final Either<Error, Content> result = Content.validateThenCreate(validContent);

        // then
        then(result.isRight()).isTrue();
        then(result.get().value()).isEqualTo(validContent);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldReturnValidationErrors_whenBlankContentProvided(final String blankContent) {
        // when
        final Either<Error, Content> result = Content.validateThenCreate(blankContent);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
        then(errors.errors().get(0)).isInstanceOf(ValidationError.Mandatory.class);
    }

}
