package com.manning.writeflow.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.manning.writeflow.application.command.CreateArticleCommand;
import com.manning.writeflow.application.command.PublishArticleCommand;
import com.manning.writeflow.application.query.GetArticleByIdQuery;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.emedina.sharedkernel.query.core.QueryBus;

import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the API interface using a REST controller.
 *
 * <p>WriteFlow is Either-only: each command and query is created through its
 * {@code validateThenCreate} factory which already returns an {@link Either}, so the result is
 * chained directly into the command or query bus and finally folded into a {@link ResponseEntity}.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Slf4j
@RestController
@RequiredArgsConstructor
final class ArticleController implements ArticleApi {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    private final ApiErrorHandler apiErrorHandler;

    /**
     * @see ArticleApi#create(ApiRequest.CreateArticle, HttpServletRequest)
     */
    @Override
    public ResponseEntity<?> create(@RequestBody final ApiRequest.CreateArticle articleRequest,
            final HttpServletRequest request) {
        final String articleId = UUID.randomUUID().toString();
        return CreateArticleCommand.validateThenCreate(articleId, articleRequest.authorId(),
                        articleRequest.title(), articleRequest.content())
                .flatMap(this.commandBus::execute)
                .mapLeft(this.apiErrorHandler::mapErrorToProblemDetail)
                .fold(
                    failure -> ApiResultUtils.createFailureResponse(failure, URI.create(request.getRequestURI())),
                        _ -> ApiResultUtils.createSuccessResponse(HttpStatus.CREATED,
                                new ApiResponse.Article(articleId, articleRequest.title(),
                                        articleRequest.authorId(), "DRAFT")));
    }

    /**
     * @see ArticleApi#publish(String, ApiRequest.PublishArticle, HttpServletRequest)
     */
    @Override
    public ResponseEntity<?> publish(@PathVariable("articleId") final String articleId,
            @RequestBody final ApiRequest.PublishArticle articleRequest, final HttpServletRequest request) {
        log.atTrace().log(Thread.currentThread().getName());
        return PublishArticleCommand.validateThenCreate(articleId, articleRequest.authorId())
                .flatMap(this.commandBus::execute)
                .mapLeft(this.apiErrorHandler::mapErrorToProblemDetail)
                .fold(
                    failure -> ApiResultUtils.createFailureResponse(failure, URI.create(request.getRequestURI())),
                        _ -> ApiResultUtils.createSuccessResponse(
                                HttpStatus.ACCEPTED,
                                null,
                                URI.create("/api/articles/" + articleId)));
    }

    /**
     * @see ArticleApi#find(String, HttpServletRequest)
     */
    @Override
    public ResponseEntity<?> find(@PathVariable("articleId") final String articleId,
            final HttpServletRequest request) {
        return GetArticleByIdQuery.validateThenCreate(articleId)
                .flatMap(this.queryBus::<Error, ArticleDTO, GetArticleByIdQuery>query)
                .mapLeft(this.apiErrorHandler::mapErrorToProblemDetail)
                .fold(
                    failure -> ApiResultUtils.createFailureResponse(failure, URI.create(request.getRequestURI())),
                        success -> ApiResultUtils.createSuccessResponse(HttpStatus.OK,
                                ApiMapper.INSTANCE.toArticleResponse(success)));
    }

}
