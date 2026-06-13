package com.manning.writeflow.application;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.query.GetArticleByIdQuery;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link GetArticleByIdHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class GetArticleByIdHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private GetArticleByIdQuery query;

    @InjectMocks
    private GetArticleByIdHandler handler;

    @Test
    void shouldReturnArticle_whenItExists() {
        // given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final ArticleDTO dto = new ArticleDTO(ARTICLE_ID, "A valid title", AUTHOR_ID, "DRAFT");

        given(this.query.articleId()).willReturn(ARTICLE_ID);
        given(this.articleRepository.findById(articleId)).willReturn(Either.right(dto));

        // when
        final Either<Error, ArticleDTO> result = this.handler.handle(this.query);

        // then
        then(result.isRight()).isTrue();
        then(result.get()).isEqualTo(dto);
        verify(this.articleRepository).findById(articleId);
    }

    @Test
    void shouldReturnNotFound_whenArticleDoesNotExist() {
        // given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final Error.BusinessError.NotFound notFound =
            new Error.BusinessError.NotFound("JpaArticleRepository", "Article", ARTICLE_ID);

        given(this.query.articleId()).willReturn(ARTICLE_ID);
        given(this.articleRepository.findById(articleId)).willReturn(Either.left(notFound));

        // when
        final Either<Error, ArticleDTO> result = this.handler.handle(this.query);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(notFound);
        verify(this.articleRepository).findById(articleId);
    }

    @Test
    void shouldReturnValidationErrors_whenIdentifierIsNotAValidUuid() {
        // given
        given(this.query.articleId()).willReturn("not-a-uuid");

        // when
        final Either<Error, ArticleDTO> result = this.handler.handle(this.query);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
    }

}
