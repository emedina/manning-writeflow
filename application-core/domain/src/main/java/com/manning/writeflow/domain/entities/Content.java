package com.manning.writeflow.domain.entities;

import java.util.List;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.Validations;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Represents the body content of an article in our Domain Model.
 *
 * <p>Content must not be blank. WriteFlow is Either-only: a blank value yields a
 * {@link Error.ValidationErrors} on the left.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Content {

    @EqualsAndHashCode.Include
    private final String value;

    /**
     * Validates the supplied value and creates a new {@link Content} instance.
     *
     * @param content the candidate content, expected to be non-blank
     * @return {@link Either#right} with the value object when valid, otherwise
     * {@link Either#left} with the accumulated {@link Error.ValidationErrors}
     */
    public static Either<Error, Content> validateThenCreate(final String content) {
        return Validations.mandatoryText(content, "content")
            .map(Content::new)
            .mapLeft(e -> new Error.ValidationErrors(List.of(e)));
    }

}
