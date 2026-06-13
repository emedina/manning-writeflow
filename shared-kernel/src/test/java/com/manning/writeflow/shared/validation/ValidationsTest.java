package com.manning.writeflow.shared.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.vavr.control.Either;

/**
 * Unit tests for the Either-returning {@link Validations} helpers.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("Validations Tests")
class ValidationsTest {

    @Nested
    @DisplayName("Given mandatoryText")
    class MandatoryTextTests {

        @Test
        @DisplayName("When text is present, then should return right with the value")
        void shouldReturnRight_whenTextPresent() {
            // when
            final Either<ValidationError, String> result = Validations.mandatoryText("hello", "title");

            // then
            then(result.isRight()).isTrue();
            then(result.get()).isEqualTo("hello");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("When text is blank, then should return left with Mandatory")
        void shouldReturnLeft_whenTextBlank(final String value) {
            // when
            final Either<ValidationError, String> result = Validations.mandatoryText(value, "title");

            // then
            then(result.isLeft()).isTrue();
            then(result.getLeft()).isInstanceOf(ValidationError.Mandatory.class);
            then(((ValidationError.Mandatory) result.getLeft()).field()).isEqualTo("title");
        }
    }

    @Nested
    @DisplayName("Given validUuid")
    class ValidUuidTests {

        @Test
        @DisplayName("When value is a valid UUID, then should return right with the value")
        void shouldReturnRight_whenValidUuid() {
            // given
            final String uuid = UUID.randomUUID().toString();

            // when
            final Either<ValidationError, String> result = Validations.validUuid(uuid, "id");

            // then
            then(result.isRight()).isTrue();
            then(result.get()).isEqualTo(uuid);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"not-a-uuid", "123"})
        @DisplayName("When value is not a UUID, then should return left with Invalid")
        void shouldReturnLeft_whenInvalidUuid(final String value) {
            // when
            final Either<ValidationError, String> result = Validations.validUuid(value, "id");

            // then
            then(result.isLeft()).isTrue();
            then(result.getLeft()).isInstanceOf(ValidationError.Invalid.class);
            then(((ValidationError.Invalid) result.getLeft()).field()).isEqualTo("id");
        }
    }

    @Nested
    @DisplayName("Given maxLength")
    class MaxLengthTests {

        @Test
        @DisplayName("When value is within bounds, then should return right")
        void shouldReturnRight_whenWithinBounds() {
            // when
            final Either<ValidationError, String> result = Validations.maxLength("abc", 5, "title");

            // then
            then(result.isRight()).isTrue();
            then(result.get()).isEqualTo("abc");
        }

        @Test
        @DisplayName("When value exceeds bounds, then should return left with MustHaveValue")
        void shouldReturnLeft_whenExceedsBounds() {
            // when
            final Either<ValidationError, String> result = Validations.maxLength("abcdef", 3, "title");

            // then
            then(result.isLeft()).isTrue();
            then(result.getLeft()).isInstanceOf(ValidationError.MustHaveValue.class);
        }

        @Test
        @DisplayName("When value is null, then should return left with MustHaveValue")
        void shouldReturnLeft_whenNull() {
            // when
            final Either<ValidationError, String> result = Validations.maxLength(null, 3, "title");

            // then
            then(result.isLeft()).isTrue();
            then(result.getLeft()).isInstanceOf(ValidationError.MustHaveValue.class);
        }
    }

    @Nested
    @DisplayName("Given mandatory")
    class MandatoryTests {

        @Test
        @DisplayName("When object is present, then should return right with the value")
        void shouldReturnRight_whenObjectPresent() {
            // when
            final Either<ValidationError, Object> result = Validations.mandatory("x", "field");

            // then
            then(result.isRight()).isTrue();
            then(result.get()).isEqualTo("x");
        }

        @Test
        @DisplayName("When object is null, then should return left with Mandatory")
        void shouldReturnLeft_whenObjectNull() {
            // when
            final Either<ValidationError, Object> result = Validations.mandatory(null, "field");

            // then
            then(result.isLeft()).isTrue();
            then(result.getLeft()).isInstanceOf(ValidationError.Mandatory.class);
            then(((ValidationError.Mandatory) result.getLeft()).field()).isEqualTo("field");
        }
    }
}
