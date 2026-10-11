package com.manning.writeflow.api;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.manning.writeflow.application.command.CreateArticleCommand;
import com.manning.writeflow.application.command.PublishArticleCommand;
import com.manning.writeflow.application.query.GetArticleByIdQuery;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.command.core.CommandBus;
import com.emedina.sharedkernel.query.core.QueryBus;

import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests for {@link ArticleController}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ArticleController Tests")
class ArticleControllerTest {

    private static final String ARTICLE_ID = UUID.randomUUID().toString();
    private static final String AUTHOR_ID = UUID.randomUUID().toString();
    private static final String TITLE = "A title";
    private static final String CONTENT = "Some content";

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    @Mock
    private ApiErrorHandler apiErrorHandler;

    @Mock
    private HttpServletRequest mockRequest;

    private ArticleController controller;

    @BeforeEach
    void setUp() {
        this.controller = new ArticleController(this.commandBus, this.queryBus, this.apiErrorHandler);
        given(this.mockRequest.getRequestURI()).willReturn("/api/articles");
    }

    @Nested
    @DisplayName("Given create method")
    class CreateMethodTests {

        @Test
        @DisplayName("When creating an article successfully, then should return CREATED with a DRAFT article")
        void shouldReturnCreated_whenCreatingArticleSuccessfully() {
            // Given
            final var articleRequest = new ApiRequest.CreateArticle(AUTHOR_ID, TITLE, CONTENT);
            given(commandBus.<Error, CreateArticleCommand>execute(any(CreateArticleCommand.class)))
                    .willReturn(Either.right(null));

            // When
            final ResponseEntity<?> response = controller.create(articleRequest, mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            then(response.getBody()).isInstanceOf(ApiResponse.Article.class);
            final var body = (ApiResponse.Article) response.getBody();
            then(body.id()).isNotNull();
            then(body.title()).isEqualTo(TITLE);
            then(body.authorId()).isEqualTo(AUTHOR_ID);
            then(body.status()).isEqualTo("DRAFT");
            verify(commandBus).execute(any(CreateArticleCommand.class));
            verifyNoInteractions(apiErrorHandler);
        }

        @Test
        @DisplayName("When the command bus fails, then should return the mapped error response")
        void shouldReturnErrorResponse_whenCommandBusFails() {
            // Given
            final var articleRequest = new ApiRequest.CreateArticle(AUTHOR_ID, TITLE, CONTENT);
            final Error error = new Error.TechnicalError.PersistenceError("JpaArticleRepository", "save", "boom");
            given(commandBus.<Error, CreateArticleCommand>execute(any(CreateArticleCommand.class)))
                    .willReturn(Either.left(error));

            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            given(apiErrorHandler.mapErrorToProblemDetail(eq(error)))
                    .willReturn(List.of(problemDetail));

            // When
            final ResponseEntity<?> response = controller.create(articleRequest, mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            then(response.getBody()).isInstanceOf(List.class);
            verify(commandBus).execute(any(CreateArticleCommand.class));
            verify(apiErrorHandler).mapErrorToProblemDetail(eq(error));
        }
    }

    @Nested
    @DisplayName("Given publish method")
    class PublishMethodTests {

        @Test
        @DisplayName("When publication is accepted, then should return ACCEPTED with a Location header")
        void publishShouldReturnAcceptedWhenRequestIsValid() {
            //given
            final var articleRequest = new ApiRequest.PublishArticle(AUTHOR_ID);
            given(commandBus.<Error, PublishArticleCommand>execute(any(PublishArticleCommand.class)))
                    .willReturn(Either.right(null));

            //when
            final ResponseEntity<?> response = controller.publish(ARTICLE_ID, articleRequest, mockRequest);

            //then
            then(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            then(response.getHeaders().getLocation()).hasToString("/api/articles/" + ARTICLE_ID);
            then(response.getBody()).isNull();
            verify(commandBus).execute(any(PublishArticleCommand.class));
        }

        @Test
        @DisplayName("When the command bus fails, then should return the mapped error response")
        void shouldReturnErrorResponse_whenCommandBusFails() {
            // Given
            final var articleRequest = new ApiRequest.PublishArticle(AUTHOR_ID);
            final Error error = new Error.BusinessError.ArticleAlreadyPublished(ARTICLE_ID);
            given(commandBus.<Error, PublishArticleCommand>execute(any(PublishArticleCommand.class)))
                    .willReturn(Either.left(error));

            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT.value());
            problemDetail.setDetail("Article with id [%s] is already published".formatted(ARTICLE_ID));
            given(apiErrorHandler.mapErrorToProblemDetail(eq(error)))
                    .willReturn(List.of(problemDetail));

            // When
            final ResponseEntity<?> response = controller.publish(ARTICLE_ID, articleRequest, mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            then(response.getBody()).isInstanceOf(List.class);
            verify(commandBus).execute(any(PublishArticleCommand.class));
            verify(apiErrorHandler).mapErrorToProblemDetail(eq(error));
        }

    }

    @Nested
    @DisplayName("Given find method")
    class FindMethodTests {

        @Test
        @DisplayName("When finding an article successfully, then should return OK with the article")
        void shouldReturnOkWithArticle_whenFindingArticleSuccessfully() {
            // Given
            final var dto = new ArticleDTO(ARTICLE_ID, "A title", AUTHOR_ID, "PUBLISHED");
            given(queryBus.<Error, ArticleDTO, GetArticleByIdQuery>query(any(GetArticleByIdQuery.class)))
                    .willReturn(Either.right(dto));

            final var expected = new ApiResponse.Article(ARTICLE_ID, "A title", AUTHOR_ID, "PUBLISHED");

            // When
            final ResponseEntity<?> response = controller.find(ARTICLE_ID, mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(response.getBody()).isInstanceOf(ApiResponse.Article.class);
            then(response.getBody()).isEqualTo(expected);
            verify(queryBus).query(any(GetArticleByIdQuery.class));
        }

        @Test
        @DisplayName("When finding an article being published, then should return PUBLISHING")
        void shouldReturnPublishingStatus_whenPublicationHasStarted() {
            //given
            final var dto = new ArticleDTO(ARTICLE_ID, TITLE, AUTHOR_ID, "PUBLISHING");
            given(queryBus.<Error, ArticleDTO, GetArticleByIdQuery>query(any(GetArticleByIdQuery.class)))
                    .willReturn(Either.right(dto));

            //when
            final ResponseEntity<?> response = controller.find(ARTICLE_ID, mockRequest);

            //then
            then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(response.getBody()).isEqualTo(new ApiResponse.Article(ARTICLE_ID, TITLE, AUTHOR_ID, "PUBLISHING"));
            verify(queryBus).query(any(GetArticleByIdQuery.class));
        }

        @Test
        @DisplayName("When the query bus fails with NotFound, then should return the mapped error response")
        void shouldReturnErrorResponse_whenQueryBusFails() {
            // Given
            final Error error = new Error.BusinessError.NotFound("JpaArticleRepository", "Article", ARTICLE_ID);
            given(queryBus.<Error, ArticleDTO, GetArticleByIdQuery>query(any(GetArticleByIdQuery.class)))
                    .willReturn(Either.left(error));

            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
            problemDetail.setDetail("Article with id [%s] not found".formatted(ARTICLE_ID));
            given(apiErrorHandler.mapErrorToProblemDetail(eq(error)))
                    .willReturn(List.of(problemDetail));

            // When
            final ResponseEntity<?> response = controller.find(ARTICLE_ID, mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            then(response.getBody()).isInstanceOf(List.class);
            verify(queryBus).query(any(GetArticleByIdQuery.class));
            verify(apiErrorHandler).mapErrorToProblemDetail(eq(error));
        }

        @Test
        @DisplayName("When the identifier is not a valid UUID, then should return BAD_REQUEST without touching the bus")
        void shouldReturnBadRequest_whenArticleIdIsInvalid() {
            // Given
            final List<ProblemDetail> problemDetails =
                    List.of(ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value()));
            given(apiErrorHandler.mapErrorToProblemDetail(any(Error.class)))
                    .willReturn(problemDetails);

            // When
            final ResponseEntity<?> response = controller.find("not-a-uuid", mockRequest);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(queryBus);
        }
    }

}
