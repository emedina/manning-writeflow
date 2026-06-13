package com.manning.writeflow.shared.error;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.manning.writeflow.shared.validation.ValidationError;

/**
 * Unit tests for the {@link Error} hierarchy.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("Error Tests")
class ErrorTest {

    @Nested
    @DisplayName("Given ValidationErrors")
    class ValidationErrorsTests {

        @Test
        @DisplayName("When creating ValidationErrors with errors, then should store them correctly")
        void shouldStoreErrors_whenValidationErrorsCreated() {
            // given
            final ValidationError ve = new ValidationError.Mandatory("title");
            final List<ValidationError> errors = Collections.singletonList(ve);

            // when
            final Error.ValidationErrors result = new Error.ValidationErrors(errors);

            // then
            then(result.errors()).containsExactly(ve);
            then(result).isInstanceOf(Error.class);
        }
    }

    @Nested
    @DisplayName("Given MultipleErrors")
    class MultipleErrorsTests {

        @Test
        @DisplayName("When creating MultipleErrors with mixed errors, then should store all of them")
        void shouldStoreErrors_whenMultipleErrorsCreated() {
            // given
            final Error business = new Error.BusinessError.NotFound("repo", "Article", "id-1");
            final Error technical = new Error.TechnicalError.SomethingWentWrong("repo", "boom");
            final List<Error> errors = Arrays.asList(business, technical);

            // when
            final Error.MultipleErrors result = new Error.MultipleErrors(errors);

            // then
            then(result.errors()).containsExactly(business, technical);
            then(result).isInstanceOf(Error.class);
        }
    }

    @Nested
    @DisplayName("Given BusinessError")
    class BusinessErrorTests {

        @Test
        @DisplayName("When creating NotFound, then should expose origin, entity and id")
        void shouldExposeFields_whenNotFoundCreated() {
            // when
            final Error.BusinessError.NotFound result = new Error.BusinessError.NotFound("repo", "Article", "id-1");

            // then
            then(result.origin()).isEqualTo("repo");
            then(result.entity()).isEqualTo("Article");
            then(result.id()).isEqualTo("id-1");
            then(result).isInstanceOf(Error.BusinessError.class);
            then(result).isInstanceOf(Error.class);
        }

        @Test
        @DisplayName("When creating ArticleAlreadyPublished, then should expose the article id")
        void shouldExposeArticleId_whenArticleAlreadyPublishedCreated() {
            // when
            final Error.BusinessError.ArticleAlreadyPublished result =
                new Error.BusinessError.ArticleAlreadyPublished("id-1");

            // then
            then(result.articleId()).isEqualTo("id-1");
            then(result).isInstanceOf(Error.BusinessError.class);
        }

        @Test
        @DisplayName("When creating AuthorNotAllowedToPublish, then should expose author and article ids")
        void shouldExposeFields_whenAuthorNotAllowedToPublishCreated() {
            // when
            final Error.BusinessError.AuthorNotAllowedToPublish result =
                new Error.BusinessError.AuthorNotAllowedToPublish("author-1", "article-1");

            // then
            then(result.authorId()).isEqualTo("author-1");
            then(result.articleId()).isEqualTo("article-1");
            then(result).isInstanceOf(Error.BusinessError.class);
        }
    }

    @Nested
    @DisplayName("Given TechnicalError")
    class TechnicalErrorTests {

        @Test
        @DisplayName("When creating PersistenceError, then should expose origin, operation and message")
        void shouldExposeFields_whenPersistenceErrorCreated() {
            // when
            final Error.TechnicalError.PersistenceError result =
                new Error.TechnicalError.PersistenceError("repo", "save", "db down");

            // then
            then(result.origin()).isEqualTo("repo");
            then(result.operation()).isEqualTo("save");
            then(result.message()).isEqualTo("db down");
            then(result).isInstanceOf(Error.TechnicalError.class);
            then(result).isInstanceOf(Error.class);
        }

        @Test
        @DisplayName("When creating SomethingWentWrong, then should expose origin and message")
        void shouldExposeFields_whenSomethingWentWrongCreated() {
            // when
            final Error.TechnicalError.SomethingWentWrong result =
                new Error.TechnicalError.SomethingWentWrong("repo", "unexpected");

            // then
            then(result.origin()).isEqualTo("repo");
            then(result.message()).isEqualTo("unexpected");
            then(result).isInstanceOf(Error.TechnicalError.class);
        }
    }

    @Nested
    @DisplayName("Given Error equality and toString")
    class EqualityTests {

        @Test
        @DisplayName("When comparing same errors, then should be equal")
        void shouldBeEqual_whenSameErrorsCompared() {
            // given
            final Error.BusinessError.NotFound a = new Error.BusinessError.NotFound("r", "Article", "id");
            final Error.BusinessError.NotFound b = new Error.BusinessError.NotFound("r", "Article", "id");

            // then
            then(a).isEqualTo(b);
            then(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("When comparing different errors, then should not be equal")
        void shouldNotBeEqual_whenDifferentErrorsCompared() {
            // given
            final Error.BusinessError.NotFound a = new Error.BusinessError.NotFound("r", "Article", "id-1");
            final Error.BusinessError.NotFound b = new Error.BusinessError.NotFound("r", "Article", "id-2");

            // then
            then(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("When calling toString, then should return a meaningful representation")
        void shouldReturnMeaningfulString_whenToStringCalled() {
            // given
            final Error.BusinessError.NotFound error = new Error.BusinessError.NotFound("r", "Article", "id-1");

            // then
            then(error.toString()).contains("NotFound").contains("Article").contains("id-1");
        }
    }
}
