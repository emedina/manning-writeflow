package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.RevertPublicationCommand;
import com.manning.writeflow.application.ports.in.RevertPublicationUseCase;

import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the revertPublication use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ApplicationService
@RequiredArgsConstructor
class RevertPublicationHandler implements RevertPublicationUseCase {

    private final ArticleRepository articleRepository;

    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final RevertPublicationCommand command) {
        return ArticleId.validateThenCreate(command.articleId())
            .flatMap(this.articleRepository::readById)
            .flatMap(article -> article.revertPublication()
                .flatMap(this.articleRepository::save));
    }

}
