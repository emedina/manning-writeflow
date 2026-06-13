package com.manning.writeflow.application.command;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link CreateArticleCommand}.
 *
 * @author Enrique Medina Montenegro
 */
class CreateArticleCommandTest {

    private static final String VALID_ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String VALID_AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String VALID_TITLE = "A meaningful title";
    private static final String VALID_CONTENT = "Some meaningful content for the article.";

    @Test
    void shouldCreateValidCommand_whenAllValidFieldsProvided() {
        // when
        final Either<Error, CreateArticleCommand> result =
            CreateArticleCommand.validateThenCreate(VALID_ARTICLE_ID, VALID_AUTHOR_ID, VALID_TITLE, VALID_CONTENT);

        // then
        then(result.isRight()).isTrue();
        final CreateArticleCommand command = result.get();
        then(command.articleId()).isEqualTo(VALID_ARTICLE_ID);
        then(command.authorId()).isEqualTo(VALID_AUTHOR_ID);
        then(command.title()).isEqualTo(VALID_TITLE);
        then(command.content()).isEqualTo(VALID_CONTENT);
    }

    @ParameterizedTest
    @CsvSource(value = {
        // invalid article identifier
        "NULL                                 , 22222222-2222-2222-2222-222222222222 , A title , Some content",
        "''                                   , 22222222-2222-2222-2222-222222222222 , A title , Some content",
        "not-a-uuid                           , 22222222-2222-2222-2222-222222222222 , A title , Some content",
        // invalid author identifier
        "11111111-1111-1111-1111-111111111111 , NULL                                 , A title , Some content",
        "11111111-1111-1111-1111-111111111111 , ''                                   , A title , Some content",
        "11111111-1111-1111-1111-111111111111 , not-a-uuid                           , A title , Some content",
        // invalid title
        "11111111-1111-1111-1111-111111111111 , 22222222-2222-2222-2222-222222222222 , NULL    , Some content",
        "11111111-1111-1111-1111-111111111111 , 22222222-2222-2222-2222-222222222222 , ''      , Some content",
        // invalid content
        "11111111-1111-1111-1111-111111111111 , 22222222-2222-2222-2222-222222222222 , A title , NULL",
        "11111111-1111-1111-1111-111111111111 , 22222222-2222-2222-2222-222222222222 , A title , ''"
    }, nullValues = "NULL")
    void shouldReturnValidationErrors_whenAnyFieldIsInvalid(final String articleId, final String authorId,
                                                            final String title, final String content) {
        // when
        final Either<Error, CreateArticleCommand> result =
            CreateArticleCommand.validateThenCreate(articleId, authorId, title, content);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldReturnValidationError_whenTitleExceedsMaximumLength() {
        // given
        final String tooLongTitle = "a".repeat(501);

        // when
        final Either<Error, CreateArticleCommand> result =
            CreateArticleCommand.validateThenCreate(VALID_ARTICLE_ID, VALID_AUTHOR_ID, tooLongTitle, VALID_CONTENT);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldAccumulateAllValidationErrors_whenEveryFieldIsInvalid() {
        // when
        final Either<Error, CreateArticleCommand> result =
            CreateArticleCommand.validateThenCreate(null, "bad", null, null);

        // then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(4);
    }

}
