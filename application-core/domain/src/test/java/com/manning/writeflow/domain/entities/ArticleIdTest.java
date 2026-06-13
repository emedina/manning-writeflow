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
 * Unit tests for {@link ArticleId}.
 *
 * @author Enrique Medina Montenegro
 */
class ArticleIdTest {

    @Test
    void shouldCreateArticleId_whenValidUuidProvided() {
        // given
        final String validId = UUID.randomUUID().toString();

        // when
        final Either<Error, ArticleId> result = ArticleId.validateThenCreate(validId);

        // then
        then(result.isRight()).isTrue();
        then(result.get().value()).isEqualTo(validId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not-a-uuid", "12345"})
    void shouldReturnValidationErrors_whenInvalidUuidProvided(final String invalidId) {
        // when
        final Either<Error, ArticleId> result = ArticleId.validateThenCreate(invalidId);

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
        final ArticleId first = ArticleId.validateThenCreate(id).get();
        final ArticleId second = ArticleId.validateThenCreate(id).get();

        // then
        then(first).isEqualTo(second);
        then(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentValue() {
        // when
        final ArticleId first = ArticleId.validateThenCreate(UUID.randomUUID().toString()).get();
        final ArticleId second = ArticleId.validateThenCreate(UUID.randomUUID().toString()).get();

        // then
        then(first).isNotEqualTo(second);
    }

}
