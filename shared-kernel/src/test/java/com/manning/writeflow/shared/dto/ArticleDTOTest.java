package com.manning.writeflow.shared.dto;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ArticleDTO}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("ArticleDTO Tests")
class ArticleDTOTest {

    @Test
    @DisplayName("When creating ArticleDTO with valid data, then should store all components")
    void shouldStoreComponents_whenValidDataProvided() {
        // when
        final ArticleDTO result = new ArticleDTO("id-1", "Title", "author-1", "DRAFT");

        // then
        then(result.id()).isEqualTo("id-1");
        then(result.title()).isEqualTo("Title");
        then(result.authorId()).isEqualTo("author-1");
        then(result.status()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("When comparing equal ArticleDTOs, then should be equal")
    void shouldBeEqual_whenSameDataProvided() {
        // given
        final ArticleDTO a = new ArticleDTO("id-1", "Title", "author-1", "DRAFT");
        final ArticleDTO b = new ArticleDTO("id-1", "Title", "author-1", "DRAFT");

        // then
        then(a).isEqualTo(b);
        then(a.hashCode()).isEqualTo(b.hashCode());
        then(a.toString()).contains("ArticleDTO").contains("id-1");
    }

    @Test
    @DisplayName("When comparing different ArticleDTOs, then should not be equal")
    void shouldNotBeEqual_whenDifferentDataProvided() {
        // given
        final ArticleDTO a = new ArticleDTO("id-1", "Title", "author-1", "DRAFT");
        final ArticleDTO b = new ArticleDTO("id-2", "Title", "author-1", "DRAFT");

        // then
        then(a).isNotEqualTo(b);
    }
}
