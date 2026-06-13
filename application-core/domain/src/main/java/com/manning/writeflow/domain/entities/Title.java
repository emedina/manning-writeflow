package com.manning.writeflow.domain.entities;

import java.util.ArrayList;
import java.util.List;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;
import com.manning.writeflow.shared.validation.Validations;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Represents the title of an article in our Domain Model.
 *
 * <p>A title must not be blank and must not exceed 500 characters. WriteFlow is
 * Either-only: field errors are accumulated and surfaced as a single
 * {@link Error.ValidationErrors} on the left.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Title {

    private static final int MAX_LENGTH = 500;

    @EqualsAndHashCode.Include
    private final String value;

    /**
     * Validates the supplied value and creates a new {@link Title} instance.
     *
     * @param title the candidate title, expected to be non-blank and at most 500 characters
     * @return {@link Either#right} with the value object when valid, otherwise
     * {@link Either#left} with the accumulated {@link Error.ValidationErrors}
     */
    public static Either<Error, Title> validateThenCreate(final String title) {
        final List<ValidationError> errors = new ArrayList<>();
        Validations.mandatoryText(title, "title").peekLeft(errors::add);
        Validations.maxLength(title, MAX_LENGTH, "title").peekLeft(errors::add);

        return errors.isEmpty()
            ? Either.right(new Title(title))
            : Either.left(new Error.ValidationErrors(List.copyOf(errors)));
    }

}
