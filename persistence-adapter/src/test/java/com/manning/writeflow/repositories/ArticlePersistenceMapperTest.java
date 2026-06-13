package com.manning.writeflow.repositories;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.shared.dto.ArticleDTO;

/**
 * Unit tests for {@link ArticlePersistenceMapper}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ArticlePersistenceMapper Tests")
class ArticlePersistenceMapperTest {

    private static final String ARTICLE_ID = UUID.randomUUID().toString();
    private static final String AUTHOR_ID = UUID.randomUUID().toString();

    private final ArticlePersistenceMapper mapper = new ArticlePersistenceMapper();

    @Test
    @DisplayName("When mapping a domain article, then it should produce an equivalent JPA entity")
    void shouldMapDomainArticleToJpaEntity() {
        // Given
        final Article article = Article.validateThenCreate(
            ArticleId.validateThenCreate(ARTICLE_ID).get(),
            Title.validateThenCreate("A Title").get(),
            Content.validateThenCreate("Some content").get(),
            AuthorId.validateThenCreate(AUTHOR_ID).get()).get();

        // When
        final JpaArticle jpa = this.mapper.toJpaEntity(article);

        // Then
        then(jpa.getId()).isEqualTo(ARTICLE_ID);
        then(jpa.getTitle()).isEqualTo("A Title");
        then(jpa.getContent()).isEqualTo("Some content");
        then(jpa.getAuthorId()).isEqualTo(AUTHOR_ID);
        then(jpa.getStatus()).isEqualTo(ArticleStatus.DRAFT.name());
    }

    @Test
    @DisplayName("When rehydrating a JPA entity, then it should produce an equivalent domain article")
    void shouldMapJpaEntityToDomainArticle() {
        // Given
        final JpaArticle jpa = new JpaArticle(ARTICLE_ID, "A Title", "Some content", AUTHOR_ID,
            ArticleStatus.PUBLISHED.name());

        // When
        final Article article = this.mapper.toDomainEntity(jpa);

        // Then
        then(article.id().value()).isEqualTo(ARTICLE_ID);
        then(article.title().value()).isEqualTo("A Title");
        then(article.content().value()).isEqualTo("Some content");
        then(article.authorId().value()).isEqualTo(AUTHOR_ID);
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When mapping a JPA entity to a DTO, then it should copy the primitive fields")
    void shouldMapJpaEntityToDto() {
        // Given
        final JpaArticle jpa = new JpaArticle(ARTICLE_ID, "A Title", "Some content", AUTHOR_ID,
            ArticleStatus.DRAFT.name());

        // When
        final ArticleDTO dto = this.mapper.toDTO(jpa);

        // Then
        then(dto.id()).isEqualTo(ARTICLE_ID);
        then(dto.title()).isEqualTo("A Title");
        then(dto.authorId()).isEqualTo(AUTHOR_ID);
        then(dto.status()).isEqualTo(ArticleStatus.DRAFT.name());
    }

    @Test
    @DisplayName("When round-tripping through JPA, then the domain article should be preserved")
    void shouldRoundTripThroughJpa() {
        // Given
        final Article original = Article.validateThenCreate(
            ArticleId.validateThenCreate(ARTICLE_ID).get(),
            Title.validateThenCreate("Round Trip").get(),
            Content.validateThenCreate("Round trip content").get(),
            AuthorId.validateThenCreate(AUTHOR_ID).get()).get();

        // When
        final Article rehydrated = this.mapper.toDomainEntity(this.mapper.toJpaEntity(original));

        // Then
        then(rehydrated).isEqualTo(original);
        then(rehydrated.status()).isEqualTo(ArticleStatus.DRAFT);
    }

}
