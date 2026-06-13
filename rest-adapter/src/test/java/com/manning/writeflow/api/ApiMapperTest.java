package com.manning.writeflow.api;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.manning.writeflow.shared.dto.ArticleDTO;

/**
 * Unit tests for {@link ApiMapper}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ApiMapper Tests")
class ApiMapperTest {

    private final ApiMapper mapper = ApiMapper.INSTANCE;

    @Nested
    @DisplayName("Given mapper instance")
    class MapperInstanceTests {

        @Test
        @DisplayName("When accessing INSTANCE, then should return a non-null mapper")
        void shouldReturnNonNullMapper_whenAccessingInstance() {
            // When & Then
            then(ApiMapper.INSTANCE).isNotNull();
            then(ApiMapper.INSTANCE).isInstanceOf(ApiMapper.class);
        }
    }

    @Nested
    @DisplayName("Given toArticleResponse method")
    class ToArticleResponseTests {

        @Test
        @DisplayName("When mapping an ArticleDTO, then should map all fields correctly")
        void shouldMapAllFields_whenMappingArticleDTO() {
            // Given
            final var dto = new ArticleDTO("article-123", "Test Title", "author-456", "PUBLISHED");

            // When
            final ApiResponse.Article result = mapper.toArticleResponse(dto);

            // Then
            then(result).isNotNull();
            then(result.id()).isEqualTo(dto.id());
            then(result.title()).isEqualTo(dto.title());
            then(result.authorId()).isEqualTo(dto.authorId());
            then(result.status()).isEqualTo(dto.status());
        }

        @Test
        @DisplayName("When mapping an ArticleDTO with null values, then should map to null values")
        void shouldMapNullValues_whenMappingArticleDTOWithNulls() {
            // Given
            final var dto = new ArticleDTO(null, null, null, null);

            // When
            final ApiResponse.Article result = mapper.toArticleResponse(dto);

            // Then
            then(result).isNotNull();
            then(result.id()).isNull();
            then(result.title()).isNull();
            then(result.authorId()).isNull();
            then(result.status()).isNull();
        }

        @Test
        @DisplayName("When mapping an ArticleDTO with unicode content, then should preserve unicode")
        void shouldPreserveUnicode_whenMappingArticleDTOWithUnicode() {
            // Given
            final var dto = new ArticleDTO("unicode-123", "Titulo con N y C", "author-1", "DRAFT");

            // When
            final ApiResponse.Article result = mapper.toArticleResponse(dto);

            // Then
            then(result).isNotNull();
            then(result.title()).isEqualTo(dto.title());
        }
    }

}
