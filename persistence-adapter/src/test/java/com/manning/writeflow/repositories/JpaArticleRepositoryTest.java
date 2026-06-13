package com.manning.writeflow.repositories;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link JpaArticleRepository}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaArticleRepository Tests")
class JpaArticleRepositoryTest {

    private static final String ARTICLE_ID = UUID.randomUUID().toString();
    private static final String AUTHOR_ID = UUID.randomUUID().toString();

    @Mock
    private SpringArticleRepository springArticleRepository;

    @Mock
    private ArticlePersistenceMapper mapper;

    @InjectMocks
    private JpaArticleRepository repository;

    private Article anArticle() {
        return Article.validateThenCreate(
            ArticleId.validateThenCreate(ARTICLE_ID).get(),
            Title.validateThenCreate("A Title").get(),
            Content.validateThenCreate("Some content").get(),
            AuthorId.validateThenCreate(AUTHOR_ID).get()).get();
    }

    private JpaArticle aJpaArticle() {
        return new JpaArticle(ARTICLE_ID, "A Title", "Some content", AUTHOR_ID, ArticleStatus.DRAFT.name());
    }

    @Test
    @DisplayName("When saving an article successfully, then it should return right and persist the entity")
    void shouldSaveSuccessfully() {
        // Given
        final Article article = anArticle();
        final JpaArticle jpa = aJpaArticle();
        given(this.mapper.toJpaEntity(article)).willReturn(jpa);

        // When
        final Either<Error, Void> result = this.repository.save(article);

        // Then
        then(result.isRight()).isTrue();
        verify(this.springArticleRepository).save(jpa);
    }

    @Test
    @DisplayName("When saving throws a data-access exception, then it should return a PersistenceError")
    void shouldReturnPersistenceErrorOnSaveFailure() {
        // Given
        final Article article = anArticle();
        final JpaArticle jpa = aJpaArticle();
        given(this.mapper.toJpaEntity(article)).willReturn(jpa);
        final DataAccessException ex = new DataAccessResourceFailureException("db down");
        given(this.springArticleRepository.save(jpa)).willThrow(ex);

        // When
        final Either<Error, Void> result = this.repository.save(article);

        // Then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.TechnicalError.PersistenceError.class);
        final var error = (Error.TechnicalError.PersistenceError) result.getLeft();
        then(error.origin()).isEqualTo("JpaArticleRepository");
        then(error.operation()).isEqualTo("save");
        then(error.message()).isEqualTo("db down");
    }

    @Test
    @DisplayName("When reading an existing article by id, then it should return the mapped domain entity")
    void shouldReadByIdWhenFound() {
        // Given
        final ArticleId id = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final JpaArticle jpa = aJpaArticle();
        final Article article = anArticle();
        given(this.springArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(jpa));
        given(this.mapper.toDomainEntity(jpa)).willReturn(article);

        // When
        final Either<Error, Article> result = this.repository.readById(id);

        // Then
        then(result.isRight()).isTrue();
        then(result.get()).isEqualTo(article);
    }

    @Test
    @DisplayName("When reading a missing article by id, then it should return a NotFound error")
    void shouldReturnNotFoundOnReadByIdWhenMissing() {
        // Given
        final ArticleId id = ArticleId.validateThenCreate(ARTICLE_ID).get();
        given(this.springArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

        // When
        final Either<Error, Article> result = this.repository.readById(id);

        // Then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.NotFound.class);
        final var error = (Error.BusinessError.NotFound) result.getLeft();
        then(error.origin()).isEqualTo("JpaArticleRepository");
        then(error.entity()).isEqualTo("Article");
        then(error.id()).isEqualTo(ARTICLE_ID);
    }

    @Test
    @DisplayName("When finding an existing article DTO by id, then it should return the mapped DTO")
    void shouldFindByIdWhenFound() {
        // Given
        final ArticleId id = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final JpaArticle jpa = aJpaArticle();
        final ArticleDTO dto = new ArticleDTO(ARTICLE_ID, "A Title", AUTHOR_ID, ArticleStatus.DRAFT.name());
        given(this.springArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(jpa));
        given(this.mapper.toDTO(jpa)).willReturn(dto);

        // When
        final Either<Error, ArticleDTO> result = this.repository.findById(id);

        // Then
        then(result.isRight()).isTrue();
        then(result.get()).isEqualTo(dto);
    }

    @Test
    @DisplayName("When finding a missing article DTO by id, then it should return a NotFound error")
    void shouldReturnNotFoundOnFindByIdWhenMissing() {
        // Given
        final ArticleId id = ArticleId.validateThenCreate(ARTICLE_ID).get();
        given(this.springArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

        // When
        final Either<Error, ArticleDTO> result = this.repository.findById(id);

        // Then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.NotFound.class);
    }

    @Test
    @DisplayName("When finding articles by author, then it should return the mapped DTOs")
    void shouldFindByAuthor() {
        // Given
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final JpaArticle jpa = aJpaArticle();
        final ArticleDTO dto = new ArticleDTO(ARTICLE_ID, "A Title", AUTHOR_ID, ArticleStatus.DRAFT.name());
        given(this.springArticleRepository.findByAuthorId(AUTHOR_ID)).willReturn(List.of(jpa));
        given(this.mapper.toDTO(jpa)).willReturn(dto);

        // When
        final Either<Error, List<ArticleDTO>> result = this.repository.findByAuthor(authorId);

        // Then
        then(result.isRight()).isTrue();
        then(result.get()).containsExactly(dto);
    }

    @Test
    @DisplayName("When finding articles by an author with none, then it should return an empty list")
    void shouldFindByAuthorWhenNone() {
        // Given
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        given(this.springArticleRepository.findByAuthorId(AUTHOR_ID)).willReturn(List.of());

        // When
        final Either<Error, List<ArticleDTO>> result = this.repository.findByAuthor(authorId);

        // Then
        then(result.isRight()).isTrue();
        then(result.get()).isEmpty();
    }

}
