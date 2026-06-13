package com.manning.writeflow.application;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.CreateArticleCommand;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import io.vavr.control.Either;

/**
 * Unit tests for {@link CreateArticleHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class CreateArticleHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String TITLE = "A perfectly valid title";
    private static final String CONTENT = "Some perfectly valid content";

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CreateArticleCommand command;

    @InjectMocks
    private CreateArticleHandler handler;

    @Test
    void shouldCreateDraftArticle_whenCommandIsValid() {
        // given
        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.title()).willReturn(TITLE);
        given(this.command.content()).willReturn(CONTENT);
        given(this.articleRepository.save(org.mockito.ArgumentMatchers.any(Article.class)))
            .willReturn(Either.right(null));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isRight()).isTrue();
        final ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(this.articleRepository).save(captor.capture());
        final Article saved = captor.getValue();
        then(saved.id().value()).isEqualTo(ARTICLE_ID);
        then(saved.authorId().value()).isEqualTo(AUTHOR_ID);
        then(saved.title().value()).isEqualTo(TITLE);
        then(saved.content().value()).isEqualTo(CONTENT);
        then(saved.status()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void shouldReturnValidationErrors_whenIdentifierIsNotAUuid() {
        // given
        given(this.command.articleId()).willReturn("not-a-uuid");

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).anyMatch(error -> error instanceof ValidationError.Invalid);
        verify(this.articleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnPersistenceError_whenSaveFails() {
        // given
        final Error.TechnicalError.PersistenceError persistenceError =
            new Error.TechnicalError.PersistenceError("JpaArticleRepository", "save", "boom");

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.title()).willReturn(TITLE);
        given(this.command.content()).willReturn(CONTENT);
        given(this.articleRepository.save(org.mockito.ArgumentMatchers.any(Article.class)))
            .willReturn(Either.left(persistenceError));

        // when
        final Either<Error, Void> result = this.handler.handle(this.command);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(persistenceError);
    }

}
