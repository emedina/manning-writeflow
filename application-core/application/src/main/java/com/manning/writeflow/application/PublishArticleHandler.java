package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.PublishArticleCommand;
import com.manning.writeflow.application.ports.in.PublishArticleUseCase;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Orchestration logic for the use case to publish an existing draft article.
 *
 * <p>The command carries primitives only, so the handler first re-parses the raw identifiers
 * into their typed value objects, reads the target article, delegates the publication decision
 * to the {@link Article} aggregate, persists the resulting state and finally drains and publishes
 * the domain events raised by the aggregate. Every step is chained over {@link Either} so
 * that any failure short-circuits the remainder of the pipeline.</p>
 *
 * @author Enrique Medina Montenegro
 * @see ApplicationService
 */
@ApplicationService
@RequiredArgsConstructor
class PublishArticleHandler implements PublishArticleUseCase {

    private final ArticleRepository articleRepository;
    private final DomainEventPublisherOutputPort domainEventPublisher;

    /**
     * Handles the command.
     *
     * @param command command to handle
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left} with the error
     */
    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final PublishArticleCommand command) {
        return ArticleId.validateThenCreate(command.articleId())
            .flatMap(articleId -> AuthorId.validateThenCreate(command.authorId())
                .flatMap(authorId -> this.articleRepository.readById(articleId)
                    .flatMap(article -> article.publish(authorId)
                        .flatMap(this.articleRepository::save)
                        .flatMap(_ -> this.domainEventPublisher.publish(article.popDomainEvents()))
                    )
                )
            );
    }

}
