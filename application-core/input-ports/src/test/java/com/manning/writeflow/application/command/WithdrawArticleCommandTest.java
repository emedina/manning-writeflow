package com.manning.writeflow.application.command;

import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for {@link WithdrawArticleCommand}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class WithdrawArticleCommandTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    @DisplayName("validateThenCreate should retain valid values")
    void validateThenCreateShouldRetainValidValues() {
        //given
        //when
        final Either<Error, WithdrawArticleCommand> result = WithdrawArticleCommand.validateThenCreate(ARTICLE_ID);

        //then
        then(result.isRight()).isTrue();
        then(result.get().articleId()).isEqualTo(ARTICLE_ID);
    }

    @Test
    @DisplayName("validateThenCreate should reject an invalid article identifier")
    void validateThenCreateShouldRejectInvalidArticleIdentifier() {
        //given
        //when
        final Either<Error, WithdrawArticleCommand> result = WithdrawArticleCommand.validateThenCreate("invalid");

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
    }

}
