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
 * Represents the unique identifier of an author in our Domain Model.
 *
 * <p>WriteFlow is Either-only: the factory returns {@link Either} with a
 * {@link Error.ValidationErrors} on the left when the supplied value is not a
 * syntactically valid UUID.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorId {

    @EqualsAndHashCode.Include
    private final String value;

    /**
     * Validates the supplied value and creates a new {@link AuthorId} instance.
     *
     * @param id the candidate identifier, expected to be a valid UUID
     * @return {@link Either#right} with the value object when valid, otherwise
     * {@link Either#left} with the accumulated {@link Error.ValidationErrors}
     */
    public static Either<Error, AuthorId> validateThenCreate(final String id) {
        return Validations.validUuid(id, "authorId")
            .map(AuthorId::new)
            .mapLeft(e -> new Error.ValidationErrors(List.of(e)));
    }

}
