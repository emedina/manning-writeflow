package com.manning.writeflow.api;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests for {@link ApiErrorHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ApiErrorHandler Tests")
class ApiErrorHandlerTest {

    private ApiErrorHandler apiErrorHandler;

    @BeforeEach
    void setUp() {
        this.apiErrorHandler = new ApiErrorHandler();
    }

    @Nested
    @DisplayName("Given ValidationErrors")
    class ValidationErrorsTests {

        @Test
        @DisplayName("When mapping a Mandatory validation error, then should return BAD_REQUEST")
        void shouldReturnBadRequest_whenMappingMandatoryValidationError() {
            // Given
            final var error = new Error.ValidationErrors(List.of(new ValidationError.Mandatory("authorId")));

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            then(result.get(0).getDetail()).contains("Mandatory field [authorId]");
        }

        @Test
        @DisplayName("When mapping several validation errors, then should join the details")
        void shouldJoinDetails_whenMappingMultipleValidationErrors() {
            // Given
            final var error = new Error.ValidationErrors(List.of(
                    new ValidationError.Invalid("articleId", "abc"),
                    new ValidationError.MustHaveValue("title", "x"),
                    new ValidationError.CannotBeEmpty("tags", List.of())));

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            then(result.get(0).getDetail()).contains("Invalid value [abc] for field [articleId]");
            then(result.get(0).getDetail()).contains("Must have value [x] for field [title]");
            then(result.get(0).getDetail()).contains("Cannot be empty [tags]");
        }
    }

    @Nested
    @DisplayName("Given BusinessError")
    class BusinessErrorTests {

        @Test
        @DisplayName("When mapping NotFound, then should return NOT_FOUND")
        void shouldReturnNotFound_whenMappingNotFound() {
            // Given
            final var error = new Error.BusinessError.NotFound("JpaArticleRepository", "Article", "id-1");

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            then(result.get(0).getDetail()).isEqualTo("Article with id [id-1] not found");
        }

        @Test
        @DisplayName("When mapping ArticleAlreadyPublished, then should return CONFLICT")
        void shouldReturnConflict_whenMappingArticleAlreadyPublished() {
            // Given
            final var error = new Error.BusinessError.ArticleAlreadyPublished("id-1");

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            then(result.get(0).getDetail()).contains("already published");
        }

        @Test
        @DisplayName("When mapping AuthorNotAllowedToPublish, then should return FORBIDDEN")
        void shouldReturnForbidden_whenMappingAuthorNotAllowedToPublish() {
            // Given
            final var error = new Error.BusinessError.AuthorNotAllowedToPublish("author-1", "id-1");

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            then(result.get(0).getDetail()).contains("not allowed to publish");
        }
    }

    @Nested
    @DisplayName("Given TechnicalError")
    class TechnicalErrorTests {

        @Test
        @DisplayName("When mapping PersistenceError, then should return INTERNAL_SERVER_ERROR")
        void shouldReturnInternalServerError_whenMappingPersistenceError() {
            // Given
            final var error = new Error.TechnicalError.PersistenceError("JpaArticleRepository", "save", "boom");

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            then(result.get(0).getDetail()).isEqualTo("Internal server error [boom]");
            then(result.get(0).getInstance()).isEqualTo(URI.create("JpaArticleRepository"));
        }

        @Test
        @DisplayName("When mapping SomethingWentWrong, then should return INTERNAL_SERVER_ERROR")
        void shouldReturnInternalServerError_whenMappingSomethingWentWrong() {
            // Given
            final var error = new Error.TechnicalError.SomethingWentWrong("origin", "oops");

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(1);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            then(result.get(0).getDetail()).isEqualTo("Internal server error [oops]");
        }
    }

    @Nested
    @DisplayName("Given MultipleErrors")
    class MultipleErrorsTests {

        @Test
        @DisplayName("When mapping MultipleErrors, then should flatten and map each error")
        void shouldFlattenAndMapEach_whenMappingMultipleErrors() {
            // Given
            final var businessError = new Error.BusinessError.NotFound("origin", "Article", "id-1");
            final var technicalError = new Error.TechnicalError.SomethingWentWrong("origin", "oops");
            final var error = new Error.MultipleErrors(List.of(businessError, technicalError));

            // When
            final List<ProblemDetail> result = apiErrorHandler.mapErrorToProblemDetail(error);

            // Then
            then(result).hasSize(2);
            then(result.get(0).getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            then(result.get(1).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

}
