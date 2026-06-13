package com.manning.writeflow.repositories;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

/**
 * Spring Data repository for {@link JpaArticle} entities.
 *
 * <p>This is the framework-level repository used internally by {@link JpaArticleRepository},
 * which adapts it to the domain {@code ArticleRepository} port.</p>
 *
 * @author Enrique Medina Montenegro
 */
public interface SpringArticleRepository extends ListCrudRepository<JpaArticle, String> {

    /**
     * Finds all persisted articles written by the supplied author.
     *
     * @param authorId the identifier of the author
     * @return the list of matching JPA articles (possibly empty)
     */
    List<JpaArticle> findByAuthorId(final String authorId);

}
