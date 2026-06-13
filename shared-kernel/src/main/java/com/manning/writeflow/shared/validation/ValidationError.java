package com.manning.writeflow.shared.validation;

import java.util.List;

/**
 * Specific errors about validations.
 *
 * @author Enrique Medina Montenegro
 */
public sealed interface ValidationError {

    record Mandatory(String field) implements ValidationError {
    }

    record MustHaveValue(String field, String value) implements ValidationError {
    }

    record CannotBeEmpty(String field, List<?> list) implements ValidationError {
    }

    record Invalid(String field, String value) implements ValidationError {
    }

}
