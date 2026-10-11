package com.manning.writeflow.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.DistributeArticleCommand;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DistributeArticleHandler}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class DistributeArticleHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String TITLE = "A perfectly valid title";
    private static final String CONTENT = "Some perfectly valid content";
    @Mock private ArticleRepository articleRepository;
    @Mock private DistributionPlatformOutputPort distributionPlatform;
    @InjectMocks private DistributeArticleHandler handler;

    @Test
    @DisplayName("handle should complete the use case")
    void handleShouldCompleteUseCase() {
        //given
        final Article article = article();
        final ArticleForDistributionDTO dto = new ArticleForDistributionDTO(ARTICLE_ID, TITLE, CONTENT, AUTHOR_ID);
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.right(article));
        given(this.distributionPlatform.distribute(dto)).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(
            DistributeArticleCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.isRight()).isTrue();
        verify(this.distributionPlatform).distribute(dto);
    }

    @Test
    @DisplayName("handle should stop after an upstream failure")
    void handleShouldStopAfterUpstreamFailure() {
        //given
        final Error error = new Error.BusinessError.NotFound("repo", "Article", ARTICLE_ID);
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(
            DistributeArticleCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isEqualTo(error);
        verify(this.distributionPlatform, never()).distribute(any());
    }

    @Test
    @DisplayName("handle should return the platform rejection")
    void handleShouldReturnPlatformRejection() {
        //given
        final Article article = article();
        final ArticleForDistributionDTO dto = new ArticleForDistributionDTO(ARTICLE_ID, TITLE, CONTENT, AUTHOR_ID);
        final Error error = new Error.BusinessError.DistributionRejected(ARTICLE_ID, "rejected");
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get()))
            .willReturn(Either.right(article));
        given(this.distributionPlatform.distribute(dto)).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(
            DistributeArticleCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isEqualTo(error);
        verify(this.distributionPlatform).distribute(dto);
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
