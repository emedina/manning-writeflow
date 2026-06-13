package com.manning.writeflow.api;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Unit tests for {@link ApiGlobalExceptionHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ApiGlobalExceptionHandler Tests")
class ApiGlobalExceptionHandlerTest {

    private ApiGlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        this.exceptionHandler = new ApiGlobalExceptionHandler();
    }

    @Nested
    @DisplayName("Given HttpMessageNotReadableException")
    class HttpMessageNotReadableExceptionTests {

        @Test
        @DisplayName("When handling a malformed body, then should return BAD_REQUEST with problem details")
        void shouldReturnBadRequest_whenHandlingHttpMessageNotReadable() {
            // Given
            final var exception = mock(HttpMessageNotReadableException.class);

            // When
            final ResponseEntity<ProblemDetail> result = exceptionHandler.handleHttpMessageNotReadable(exception);

            // Then
            then(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            then(result.getBody()).isNotNull();
            then(result.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            then(result.getBody().getTitle()).isEqualTo("Bad Request");
            then(result.getBody().getDetail())
                    .isEqualTo("Invalid request body: malformed JSON or missing required fields");
        }
    }

    @Nested
    @DisplayName("Given IllegalArgumentException")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("When handling an illegal argument, then should return INTERNAL_SERVER_ERROR")
        void shouldReturnInternalServerError_whenHandlingIllegalArgument() {
            // Given
            final var exception = new IllegalArgumentException("No handler found for command");

            // When
            final ResponseEntity<ProblemDetail> result = exceptionHandler.handleIllegalArgument(exception);

            // Then
            then(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            then(result.getBody()).isNotNull();
            then(result.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            then(result.getBody().getTitle()).isEqualTo("Internal Server Error");
            then(result.getBody().getDetail()).isEqualTo("Internal configuration error");
        }
    }

    @Nested
    @DisplayName("Given an unexpected exception")
    class UnexpectedExceptionTests {

        @Test
        @DisplayName("When handling a generic exception, then should return INTERNAL_SERVER_ERROR")
        void shouldReturnInternalServerError_whenHandlingGenericException() {
            // Given
            final var exception = new Exception("Unexpected error occurred");

            // When
            final ResponseEntity<ProblemDetail> result = exceptionHandler.handleUnexpectedException(exception);

            // Then
            then(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            then(result.getBody()).isNotNull();
            then(result.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            then(result.getBody().getTitle()).isEqualTo("Internal Server Error");
            then(result.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        }
    }

}
