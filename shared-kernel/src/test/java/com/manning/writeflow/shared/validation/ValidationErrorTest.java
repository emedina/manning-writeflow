package com.manning.writeflow.shared.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ValidationError} hierarchy.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ValidationError Tests")
class ValidationErrorTest {

    @Test
    @DisplayName("When creating Mandatory, then should expose the field")
    void shouldExposeField_whenMandatoryCreated() {
        // when
        final ValidationError.Mandatory result = new ValidationError.Mandatory("title");

        // then
        then(result.field()).isEqualTo("title");
        then(result).isInstanceOf(ValidationError.class);
    }

    @Test
    @DisplayName("When creating MustHaveValue, then should expose field and value")
    void shouldExposeFields_whenMustHaveValueCreated() {
        // when
        final ValidationError.MustHaveValue result = new ValidationError.MustHaveValue("title", "x");

        // then
        then(result.field()).isEqualTo("title");
        then(result.value()).isEqualTo("x");
        then(result).isInstanceOf(ValidationError.class);
    }

    @Test
    @DisplayName("When creating CannotBeEmpty, then should expose field and list")
    void shouldExposeFields_whenCannotBeEmptyCreated() {
        // given
        final List<String> list = Arrays.asList("a", "b");

        // when
        final ValidationError.CannotBeEmpty result = new ValidationError.CannotBeEmpty("items", list);

        // then
        then(result.field()).isEqualTo("items");
        then(result.list()).isEqualTo(list);
        then(result).isInstanceOf(ValidationError.class);
    }

    @Test
    @DisplayName("When creating Invalid, then should expose field and value")
    void shouldExposeFields_whenInvalidCreated() {
        // when
        final ValidationError.Invalid result = new ValidationError.Invalid("id", "not-a-uuid");

        // then
        then(result.field()).isEqualTo("id");
        then(result.value()).isEqualTo("not-a-uuid");
        then(result).isInstanceOf(ValidationError.class);
    }

    @Test
    @DisplayName("When comparing equal validation errors, then should be equal")
    void shouldBeEqual_whenSameValidationErrorsCompared() {
        // given
        final ValidationError.Mandatory a = new ValidationError.Mandatory("title");
        final ValidationError.Mandatory b = new ValidationError.Mandatory("title");

        // then
        then(a).isEqualTo(b);
        then(a.hashCode()).isEqualTo(b.hashCode());
        then(a.toString()).contains("Mandatory").contains("title");
    }
}
