package com.manning.writeflow.api;

import static org.assertj.core.api.BDDAssertions.then;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link ApiResultUtils}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ApiResultUtils Tests")
class ApiResultUtilsTest {

    @Nested
    @DisplayName("Given createSuccessResponse")
    class CreateSuccessResponseTests {

        @Test
        @DisplayName("When creating a success response with a body, then should carry the status and body")
        void shouldCarryStatusAndBody_whenCreatingSuccessResponse() {
            // Given
            final var body = new ApiResponse.Article("id-1", "title", "author-1", "PUBLISHED");

            // When
            final ResponseEntity<ApiResponse> response =
                    ApiResultUtils.createSuccessResponse(HttpStatus.OK, body);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(response.getBody()).isEqualTo(body);
        }

        @Test
        @DisplayName("When creating a success response with no body, then should carry the status only")
        void shouldCarryStatusOnly_whenCreatingSuccessResponseWithNoBody() {
            // When
            final ResponseEntity<ApiResponse> response =
                    ApiResultUtils.createSuccessResponse(HttpStatus.NO_CONTENT, null);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            then(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("Given createFailureResponse")
    class CreateFailureResponseTests {

        @Test
        @DisplayName("When there is a single problem, then should use that problem status and set its type")
        void shouldUseSingleProblemStatus_whenThereIsOneProblem() {
            // Given
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
            final URI uri = URI.create("/api/articles/id-1");

            // When
            final ResponseEntity<List<ProblemDetail>> response =
                    ApiResultUtils.createFailureResponse(List.of(problemDetail), uri);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            then(response.getBody()).hasSize(1);
            then(response.getBody().get(0).getType()).isEqualTo(uri);
        }

        @Test
        @DisplayName("When there are several problems, then should fall back to I_AM_A_TEAPOT")
        void shouldFallBackToTeapot_whenThereAreSeveralProblems() {
            // Given
            final ProblemDetail first = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
            final ProblemDetail second = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            final URI uri = URI.create("/api/articles/id-1");

            // When
            final ResponseEntity<List<ProblemDetail>> response =
                    ApiResultUtils.createFailureResponse(List.of(first, second), uri);

            // Then
            then(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
            then(response.getBody()).hasSize(2);
        }
    }

}
