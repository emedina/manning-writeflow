package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.ConfirmPublicationCommand;
import com.manning.writeflow.application.ports.in.ConfirmPublicationUseCase;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the confirmPublication use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ApplicationService
@RequiredArgsConstructor
class ConfirmPublicationHandler implements ConfirmPublicationUseCase {

    private final ArticleRepository articleRepository;
    private final DomainEventPublisherOutputPort domainEventPublisher;

    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final ConfirmPublicationCommand command) {
        return ArticleId.validateThenCreate(command.articleId())
            .flatMap(this.articleRepository::readById)
            .flatMap(article -> article.confirmPublication(command.publishedAt())
                .flatMap(this.articleRepository::save)
                .flatMap(_ -> this.domainEventPublisher.publish(article.popDomainEvents())));
    }

}
