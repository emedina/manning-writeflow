package com.manning.writeflow.shared.error;

import java.io.Serializable;
import java.util.List;

import com.manning.writeflow.shared.validation.ValidationError;

/**
 * Hierarchy of possible error values for WriteFlow.
 *
 * @author Enrique Medina Montenegro
 */
public sealed interface Error extends Serializable {

    record ValidationErrors(List<ValidationError> errors) implements Error {
    }

    record MultipleErrors(List<Error> errors) implements Error {
    }

    sealed interface BusinessError extends Error {

        record NotFound(String origin, String entity, String id) implements BusinessError {
        }

        record ArticleAlreadyPublished(String articleId) implements BusinessError {
        }

        record AuthorNotAllowedToPublish(String authorId, String articleId) implements BusinessError {
        }

        record DistributionRejected(String articleId, String reason) implements BusinessError {
        }

    }

    sealed interface TechnicalError extends Error {

        record PersistenceError(String origin, String operation, String message) implements TechnicalError {
        }

        record SomethingWentWrong(String origin, String message) implements TechnicalError {
        }

    }

}
