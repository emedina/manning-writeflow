package com.manning.writeflow.application.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for {@link ConfirmPublicationCommand}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class ConfirmPublicationCommandTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    @DisplayName("validateThenCreate should retain valid values")
    void validateThenCreateShouldRetainValidValues() {
        //given
        //when
        final Either<Error, ConfirmPublicationCommand> result = ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, PUBLISHED_AT);

        //then
        then(result.isRight()).isTrue();
        then(result.get().articleId()).isEqualTo(ARTICLE_ID);
        then(result.get().publishedAt()).isEqualTo(PUBLISHED_AT);
    }

    @Test
    @DisplayName("validateThenCreate should reject an invalid article identifier")
    void validateThenCreateShouldRejectInvalidArticleIdentifier() {
        //given
        //when
        final Either<Error, ConfirmPublicationCommand> result = ConfirmPublicationCommand.validateThenCreate("invalid", PUBLISHED_AT);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
    }

    @Test
    @DisplayName("validateThenCreate should reject a missing publication instant")
    void validateThenCreateShouldRejectMissingPublicationInstant() {
        //given
        //when
        final Either<Error, ConfirmPublicationCommand> result =
            ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, null);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
    }

}
