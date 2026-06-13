package com.manning.writeflow.api;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.validation.ValidationError;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * API error handler that maps error values to HTTP status codes and problem details
 * using a list of {@link ProblemDetail}.
 *
 * @author Enrique Medina Montenegro
 */
@Slf4j
@Component
final class ApiErrorHandler {

    /**
     * Maps a WriteFlow {@link Error} to one or more RFC 7807 {@link ProblemDetail} objects.
     *
     * @param error the error value to map
     * @return the list of problem details (one per leaf error)
     */
    List<ProblemDetail> mapErrorToProblemDetail(final Error error) {
        return switch (error) {
            case Error.ValidationErrors validationErrors ->
                    List.of(this.createProblemDetailFromValidationErrors(validationErrors));
            case Error.MultipleErrors multipleErrors ->
                    this.createProblemDetailFromMultipleErrors(multipleErrors);
            case Error.BusinessError businessError ->
                    List.of(this.createProblemDetailFromBusinessError(businessError));
            case Error.TechnicalError technicalError ->
                    List.of(this.createProblemDetailFromTechnicalError(technicalError));
        };
    }

    private ProblemDetail createProblemDetailFromBusinessError(final Error.BusinessError businessError) {
        log.atTrace().setMessage("Business error received").log();
        return switch (businessError) {
            case Error.BusinessError.NotFound notFound -> {
                final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND.value());
                problemDetail.setDetail("%s with id [%s] not found".formatted(notFound.entity(), notFound.id()));
                yield problemDetail;
            }
            case Error.BusinessError.ArticleAlreadyPublished alreadyPublished -> {
                final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT.value());
                problemDetail.setDetail("Article with id [%s] is already published"
                        .formatted(alreadyPublished.articleId()));
                yield problemDetail;
            }
            case Error.BusinessError.AuthorNotAllowedToPublish notAllowed -> {
                final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN.value());
                problemDetail.setDetail("Author [%s] is not allowed to publish article [%s]"
                        .formatted(notAllowed.authorId(), notAllowed.articleId()));
                yield problemDetail;
            }
        };
    }

    private ProblemDetail createProblemDetailFromTechnicalError(final Error.TechnicalError technicalError) {
        log.atTrace().setMessage("Technical error received").log();
        return switch (technicalError) {
            case Error.TechnicalError.PersistenceError persistenceError -> {
                final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                problemDetail.setInstance(URI.create(persistenceError.origin()));
                problemDetail.setDetail("Internal server error [%s]".formatted(persistenceError.message()));
                yield problemDetail;
            }
            case Error.TechnicalError.SomethingWentWrong somethingWentWrong -> {
                final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                problemDetail.setInstance(URI.create(somethingWentWrong.origin()));
                problemDetail.setDetail("Internal server error [%s]".formatted(somethingWentWrong.message()));
                yield problemDetail;
            }
        };
    }

    private ProblemDetail createProblemDetailFromValidationErrors(final Error.ValidationErrors validationErrors) {
        log.atTrace().setMessage("Validation errors received, continue to map list of [{}]")
                .addArgument(validationErrors.errors()::size).log();
        final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
        problemDetail.setDetail(validationErrors.errors().stream()
                .map(this::createDetailFromValidationError).collect(Collectors.joining(", ")));

        return problemDetail;
    }

    private String createDetailFromValidationError(final ValidationError validationError) {
        return switch (validationError) {
            case ValidationError.Mandatory mandatory -> "Mandatory field [%s]".formatted(mandatory.field());
            case ValidationError.MustHaveValue mustHaveValue ->
                    "Must have value [%s] for field [%s]".formatted(mustHaveValue.value(), mustHaveValue.field());
            case ValidationError.CannotBeEmpty cannotBeEmpty ->
                    "Cannot be empty [%s]".formatted(cannotBeEmpty.field());
            case ValidationError.Invalid invalid ->
                    "Invalid value [%s] for field [%s]".formatted(invalid.value(), invalid.field());
        };
    }

    private List<ProblemDetail> createProblemDetailFromMultipleErrors(final Error.MultipleErrors multipleErrors) {
        log.atTrace().setMessage("Multiple errors received").log();
        return multipleErrors.errors().stream()
                .map(this::mapErrorToProblemDetail)
                .flatMap(List::stream)
                .toList();
    }

}
