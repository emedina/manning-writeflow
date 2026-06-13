package com.manning.writeflow.repositories;

import org.springframework.stereotype.Component;

import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.shared.dto.ArticleDTO;

/**
 * Maps between the domain {@code Article} aggregate, its JPA persistence representation
 * {@link JpaArticle}, and the {@link ArticleDTO} read model.
 *
 * <p>Rehydration uses {@link Article#reconstitute} together with the value objects'
 * {@code validateThenCreate(...).get()} factories: persisted data is trusted to be valid,
 * so the {@link io.vavr.control.Either} right-projection is unwrapped directly.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Component
class ArticlePersistenceMapper {

    /**
     * Maps a domain article to its JPA persistence representation.
     *
     * @param article the domain article
     * @return the JPA entity
     */
    JpaArticle toJpaEntity(final Article article) {
        return new JpaArticle(
            article.id().value(),
            article.title().value(),
            article.content().value(),
            article.authorId().value(),
            article.status().name());
    }

    /**
     * Rehydrates a domain article from its JPA persistence representation.
     *
     * @param jpaArticle the JPA entity
     * @return the reconstituted domain article
     */
    Article toDomainEntity(final JpaArticle jpaArticle) {
        return Article.reconstitute(
            ArticleId.validateThenCreate(jpaArticle.getId()).get(),
            Title.validateThenCreate(jpaArticle.getTitle()).get(),
            Content.validateThenCreate(jpaArticle.getContent()).get(),
            AuthorId.validateThenCreate(jpaArticle.getAuthorId()).get(),
            ArticleStatus.valueOf(jpaArticle.getStatus()));
    }

    /**
     * Maps a JPA persistence representation directly to the read-model DTO.
     *
     * @param jpaArticle the JPA entity
     * @return the article DTO
     */
    ArticleDTO toDTO(final JpaArticle jpaArticle) {
        return new ArticleDTO(
            jpaArticle.getId(),
            jpaArticle.getTitle(),
            jpaArticle.getAuthorId(),
            jpaArticle.getStatus());
    }

}
