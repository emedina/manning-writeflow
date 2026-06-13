package com.manning.writeflow.shared.validation;

import java.util.Objects;
import java.util.UUID;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to provide Either-returning validation helpers.
 *
 * <p>WriteFlow is Either-only: these helpers never use Vavr's {@code Validation} type.
 * Each helper returns an {@link Either} whose left side is a {@link ValidationError} and
 * whose right side is the validated value, so that value objects, commands and queries can
 * accumulate field errors before deciding the outcome at their public boundary.</p>
 *
 * @author Enrique Medina Montenegro
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Validations {

    /**
     * Validates that the provided text is neither {@code null} nor blank.
     *
     * @param value the text to be validated
     * @param field the logical name of the field being validated
     * @return {@link Either#right} with the value when present, otherwise
     * {@link Either#left} with a {@link ValidationError.Mandatory}
     */
    public static Either<ValidationError, String> mandatoryText(final String value, final String field) {
        return Objects.nonNull(value) && !value.isBlank()
            ? Either.right(value)
            : Either.left(new ValidationError.Mandatory(field));
    }

    /**
     * Validates that the provided text is a syntactically valid UUID.
     *
     * @param value the text to be validated
     * @param field the logical name of the field being validated
     * @return {@link Either#right} with the value when it is a valid UUID, otherwise
     * {@link Either#left} with a {@link ValidationError.Invalid}
     */
    public static Either<ValidationError, String> validUuid(final String value, final String field) {
        if (Objects.isNull(value) || value.isBlank()) {
            return Either.left(new ValidationError.Invalid(field, value));
        }
        try {
            UUID.fromString(value);
            return Either.right(value);
        } catch (final IllegalArgumentException e) {
            return Either.left(new ValidationError.Invalid(field, value));
        }
    }

    /**
     * Validates that the provided text does not exceed the given maximum length.
     *
     * @param value the text to be validated
     * @param max the maximum allowed length (inclusive)
     * @param field the logical name of the field being validated
     * @return {@link Either#right} with the value when within bounds, otherwise
     * {@link Either#left} with a {@link ValidationError.MustHaveValue}
     */
    public static Either<ValidationError, String> maxLength(final String value, final int max, final String field) {
        return Objects.nonNull(value) && value.length() <= max
            ? Either.right(value)
            : Either.left(new ValidationError.MustHaveValue(field, value));
    }

    /**
     * Validates that the provided object is not {@code null}.
     *
     * @param value the object to be validated
     * @param field the logical name of the field being validated
     * @param <T> the type of the object being validated
     * @return {@link Either#right} with the value when present, otherwise
     * {@link Either#left} with a {@link ValidationError.Mandatory}
     */
    public static <T> Either<ValidationError, T> mandatory(final T value, final String field) {
        return Objects.nonNull(value)
            ? Either.right(value)
            : Either.left(new ValidationError.Mandatory(field));
    }

}
