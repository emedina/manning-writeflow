package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.manning.writeflow.application.command.DistributeArticleCommand;
import com.manning.writeflow.application.ports.in.DistributeArticleUseCase;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the distributeArticle use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ApplicationService
@RequiredArgsConstructor
class DistributeArticleHandler implements DistributeArticleUseCase {

    private final ArticleRepository articleRepository;
    private final DistributionPlatformOutputPort distributionPlatform;

    @Override
    public Either<Error, Void> handle(final DistributeArticleCommand command) {
        return ArticleId.validateThenCreate(command.articleId())
            .flatMap(this.articleRepository::readById)
            .map(article -> new ArticleForDistributionDTO(
                article.id().value(), article.title().value(), article.content().value(), article.authorId().value()))
            .flatMap(this.distributionPlatform::distribute);
    }

}
