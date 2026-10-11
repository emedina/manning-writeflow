package com.manning.writeflow.repositories;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter implementing the domain {@link ArticleRepository} port on top of Spring
 * Data JPA.
 *
 * <p>This adapter is the only place that knows about JPA. It translates between the domain
 * model and the {@link JpaArticle} persistence model via {@link ArticlePersistenceMapper}, maps
 * a missing article to {@link Error.BusinessError.NotFound} (R6: {@code readById} returns a bare
 * {@link Either}, never {@code Optional}), and converts infrastructure failures into
 * {@link Error.TechnicalError.PersistenceError}. Optimistic-locking behavior is demonstrated by
 * running the application and is not covered by an automated test.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Adapter
@RequiredArgsConstructor
class JpaArticleRepository implements ArticleRepository {

    private static final String ORIGIN = "JpaArticleRepository";
    private static final String ENTITY = "Article";

    private final SpringArticleRepository springArticleRepository;
    private final ArticlePersistenceMapper mapper;

    /**
     * Persists the supplied article, translating any data-access failure into a
     * {@link Error.TechnicalError.PersistenceError}.
     *
     * @param article the article to persist
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left}
     * with a persistence error
     */
    @Override
    public Either<Error, Void> save(final Article article) {
        try {
            final JpaArticle managedEntity = this.springArticleRepository.findById(article.id().value())
                .orElseGet(() -> this.mapper.toJpaEntity(article));
            this.updateManagedEntity(managedEntity, article);
            this.springArticleRepository.save(managedEntity);

            return Either.right(null);
        } catch (final OptimisticLockingFailureException e) {
            return Either.left(new Error.TechnicalError.PersistenceError(ORIGIN, "save", e.getMessage()));
        } catch (final DataAccessException e) {
            return Either.left(new Error.TechnicalError.PersistenceError(ORIGIN, "save", e.getMessage()));
        }
    }

    /**
     * Reads the article with the supplied identifier as a domain entity.
     *
     * @param id the identifier of the article
     * @return {@link Either#right} with the article, or {@link Either#left} with a
     * {@link Error.BusinessError.NotFound} when no such article exists
     */
    @Override
    public Either<Error, Article> readById(final ArticleId id) {
        return this.springArticleRepository.findById(id.value())
            .<Either<Error, Article>>map(jpa -> Either.right(this.mapper.toDomainEntity(jpa)))
            .orElseGet(() -> Either.left(new Error.BusinessError.NotFound(ORIGIN, ENTITY, id.value())));
    }

    /**
     * Finds the article with the supplied identifier as a DTO.
     *
     * @param id the identifier of the article
     * @return {@link Either#right} with the article DTO, or {@link Either#left} with a
     * {@link Error.BusinessError.NotFound} when no such article exists
     */
    @Override
    public Either<Error, ArticleDTO> findById(final ArticleId id) {
        return this.springArticleRepository.findById(id.value())
            .<Either<Error, ArticleDTO>>map(jpa -> Either.right(this.mapper.toDTO(jpa)))
            .orElseGet(() -> Either.left(new Error.BusinessError.NotFound(ORIGIN, ENTITY, id.value())));
    }

    /**
     * Finds all articles written by the supplied author as DTOs.
     *
     * @param authorId the identifier of the author
     * @return {@link Either#right} with the list of article DTOs (possibly empty)
     */
    @Override
    public Either<Error, List<ArticleDTO>> findByAuthor(final AuthorId authorId) {
        final List<ArticleDTO> dtos = this.springArticleRepository.findByAuthorId(authorId.value()).stream()
            .map(this.mapper::toDTO)
            .toList();

        return Either.right(dtos);
    }

    private void updateManagedEntity(final JpaArticle managedEntity, final Article article) {
        managedEntity.setTitle(article.title().value());
        managedEntity.setContent(article.content().value());
        managedEntity.setAuthorId(article.authorId().value());
        managedEntity.setStatus(article.status().name());
    }

}
