package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.ports.in.GetArticleByIdUseCase;
import com.manning.writeflow.application.query.GetArticleByIdQuery;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Orchestration logic for the use case to retrieve an article by its identifier.
 *
 * <p>The raw identifier carried by the query is re-validated into the typed
 * {@link ArticleId} value object before delegating to the repository, so that the read
 * path remains Either-only end to end.</p>
 *
 * @author Enrique Medina Montenegro
 * @see ApplicationService
 */
@ApplicationService
@RequiredArgsConstructor
class GetArticleByIdHandler implements GetArticleByIdUseCase {

    private final ArticleRepository articleRepository;

    /**
     * Handles the query.
     *
     * @param query query to handle
     * @return {@link Either#right} with the article DTO, otherwise {@link Either#left} with the error
     */
    @Override
    @Transactional(readOnly = true)
    public Either<Error, ArticleDTO> handle(final GetArticleByIdQuery query) {
        return ArticleId.validateThenCreate(query.articleId())
            .flatMap(this.articleRepository::findById);
    }

}
