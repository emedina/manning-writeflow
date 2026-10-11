package com.manning.writeflow.shared.dto;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArticleForDistributionDTO}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class ArticleForDistributionDTOTest {

    @Test
    @DisplayName("ArticleForDistributionDTO should retain all Distribution Platform fields")
    void shouldRetainAllFieldsWhenCreated() {
        //given
        final ArticleForDistributionDTO article =
            new ArticleForDistributionDTO("article-1", "Title", "Content", "author-1");

        //then
        then(article.id()).isEqualTo("article-1");
        then(article.title()).isEqualTo("Title");
        then(article.content()).isEqualTo("Content");
        then(article.authorId()).isEqualTo("author-1");
    }

}
