package com.manning.writeflow.application;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manning.writeflow.application.command.RevertPublicationCommand;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RevertPublicationHandler}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class RevertPublicationHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String TITLE = "A perfectly valid title";
    private static final String CONTENT = "Some perfectly valid content";
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T03:04:05Z");
    @Mock private ArticleRepository articleRepository;
    @InjectMocks private RevertPublicationHandler handler;

    @Test
    @DisplayName("handle should complete the use case")
    void handleShouldCompleteUseCase() {
        //given
        final Article article = article();
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.right(article));
        given(this.articleRepository.save(article)).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(
            RevertPublicationCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.isRight()).isTrue();
        then(article.status()).isEqualTo(ArticleStatus.DRAFT);
        verify(this.articleRepository).save(article);
    }

    @Test
    @DisplayName("handle should stop after an upstream failure")
    void handleShouldStopAfterUpstreamFailure() {
        //given
        final Error error = new Error.BusinessError.NotFound("repo", "Article", ARTICLE_ID);
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(
            RevertPublicationCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isEqualTo(error);
        verify(this.articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle should return the persistence error when save fails")
    void handleShouldReturnPersistenceErrorWhenSaveFails() {
        //given
        final Article article = article();
        final Error error = new Error.TechnicalError.PersistenceError("repo", "save", "failed");
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get()))
            .willReturn(Either.right(article));
        given(this.articleRepository.save(article)).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(RevertPublicationCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isEqualTo(error);
    }

    @Test
    @DisplayName("handle should reject reversion after publication is confirmed")
    void handleShouldRejectReversionAfterPublicationIsConfirmed() {
        //given
        final Article article = Article.validateThenCreate(
            ArticleId.validateThenCreate(ARTICLE_ID).get(),
            Title.validateThenCreate(TITLE).get(),
            Content.validateThenCreate(CONTENT).get(),
            AuthorId.validateThenCreate(AUTHOR_ID).get()).get();
        article.startPublication(AuthorId.validateThenCreate(AUTHOR_ID).get());
        article.confirmPublication(PUBLISHED_AT);
        article.popDomainEvents();
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get()))
            .willReturn(Either.right(article));

        //when
        final Either<Error, Void> result = this.handler.handle(RevertPublicationCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isInstanceOf(Error.BusinessError.ArticleAlreadyPublished.class);
        verify(this.articleRepository, never()).save(any());
    }

    private static Article article() {
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Title title = Title.validateThenCreate(TITLE).get();
        final Content content = Content.validateThenCreate(CONTENT).get();
        final Article article = Article.validateThenCreate(articleId, title, content, authorId).get();
        article.startPublication(authorId);
        return article;
    }

}
