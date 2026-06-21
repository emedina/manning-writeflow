package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.emedina.sharedkernel.transactional.Transactional;
import com.manning.writeflow.application.command.CreateArticleCommand;
import com.manning.writeflow.application.ports.in.CreateArticleUseCase;
import com.manning.writeflow.domain.entities.*;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Orchestration logic for the use case to create a new draft article.
 *
 * <p>The command carries primitives only, so the handler re-parses the raw identifiers and
 * text fields into their typed value objects, assembles the {@link Article} aggregate in
 * {@link com.manning.writeflow.domain.entities.ArticleStatus#DRAFT} and persists it. Every
 * step is chained over {@link Either} so that any failure short-circuits the remainder of the
 * pipeline. No domain event is raised on creation; only publication raises
 * {@link com.manning.writeflow.shared.event.ArticlePublished}.</p>
 *
 * @author Enrique Medina Montenegro
 * @see ApplicationService
 */
@ApplicationService
@RequiredArgsConstructor
class CreateArticleHandler implements CreateArticleUseCase {

    private final ArticleRepository articleRepository;

    /**
     * Handles the command.
     *
     * @param command command to handle
     * @return {@link Either#right} with {@code null} on success, otherwise {@link Either#left} with the error
     */
    @Override
    @Transactional(rollbackForClassNameWithEither = { "com.manning.writeflow.shared.error.Error" })
    public Either<Error, Void> handle(final CreateArticleCommand command) {
        return ArticleId.validateThenCreate(command.articleId())
            .flatMap(articleId -> AuthorId.validateThenCreate(command.authorId())
                .flatMap(authorId -> Title.validateThenCreate(command.title())
                    .flatMap(title -> Content.validateThenCreate(command.content())
                        .flatMap(content -> Article.validateThenCreate(articleId, title, content, authorId)))))
            .flatMap(this.articleRepository::save);
    }

}
