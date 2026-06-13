package com.manning.writeflow.domain.entities;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import io.vavr.control.Either;

/**
 * Unit tests for {@link AuthorId}.
 *
 * @author Enrique Medina Montenegro
 */
class AuthorIdTest {

    @Test
    void shouldCreateAuthorId_whenValidUuidProvided() {
        // given
        final String validId = UUID.randomUUID().toString();

        // when
        final Either<Error, AuthorId> result = AuthorId.validateThenCreate(validId);

        // then
        then(result.isRight()).isTrue();
        then(result.get().value()).isEqualTo(validId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not-a-uuid", "abc-123"})
    void shouldReturnValidationErrors_whenInvalidUuidProvided(final String invalidId) {
        // when
        final Either<Error, AuthorId> result = AuthorId.validateThenCreate(invalidId);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
        then(errors.errors().get(0)).isInstanceOf(ValidationError.Invalid.class);
    }

    @Test
    void shouldBeEqual_whenSameValue() {
        // given
        final String id = UUID.randomUUID().toString();

        // when
        final AuthorId first = AuthorId.validateThenCreate(id).get();
        final AuthorId second = AuthorId.validateThenCreate(id).get();

        // then
        then(first).isEqualTo(second);
        then(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentValue() {
        // when
        final AuthorId first = AuthorId.validateThenCreate(UUID.randomUUID().toString()).get();
        final AuthorId second = AuthorId.validateThenCreate(UUID.randomUUID().toString()).get();

        // then
        then(first).isNotEqualTo(second);
    }

}
