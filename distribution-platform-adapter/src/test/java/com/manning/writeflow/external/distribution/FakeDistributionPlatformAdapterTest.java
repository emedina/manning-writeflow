package com.manning.writeflow.external.distribution;

import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link FakeDistributionPlatformAdapter}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class FakeDistributionPlatformAdapterTest {

    private static final ArticleForDistributionDTO ARTICLE = new ArticleForDistributionDTO(
        "article-1", "Title", "Content", "author-1");

    @Test
    @DisplayName("distribute should remain idempotent for repeated article identifiers")
    void distributeShouldRemainIdempotentForRepeatedArticleIdentifiers() {
        //given
        final FakeDistributionPlatformAdapter adapter = new FakeDistributionPlatformAdapter();

        //when
        final Either<Error, Void> firstResult = adapter.distribute(ARTICLE);
        final Either<Error, Void> secondResult = adapter.distribute(ARTICLE);

        //then
        then(firstResult.isRight()).isTrue();
        then(secondResult.isRight()).isTrue();
        then(adapter.hasDistributed(ARTICLE.id())).isTrue();
    }

    @Test
    @DisplayName("distribute should return the programmed next failure")
    void distributeShouldReturnProgrammedNextFailure() {
        //given
        final FakeDistributionPlatformAdapter adapter = new FakeDistributionPlatformAdapter();
        final Error.BusinessError.DistributionRejected failure =
            new Error.BusinessError.DistributionRejected(ARTICLE.id(), "refused");
        adapter.failNextWith(failure);

        //when
        final Either<Error, Void> result = adapter.distribute(ARTICLE);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(failure);
        then(adapter.hasDistributed(ARTICLE.id())).isFalse();
    }

    @Test
    @DisplayName("distribute should reject once when the demo property is enabled")
    void distributeShouldRejectOnceWhenDemoPropertyIsEnabled() {
        //given
        final String property = "writeflow.fake-distribution.reject-next";
        System.setProperty(property, "true");
        final FakeDistributionPlatformAdapter adapter = new FakeDistributionPlatformAdapter();
        System.clearProperty(property);

        //when
        final Either<Error, Void> rejected = adapter.distribute(ARTICLE);
        final Either<Error, Void> accepted = adapter.distribute(ARTICLE);

        //then
        then(rejected.isLeft()).isTrue();
        then(accepted.isRight()).isTrue();
        then(adapter.hasDistributed(ARTICLE.id())).isTrue();
    }

    @Test
    @DisplayName("withdraw should succeed when the article was never distributed")
    void withdrawShouldSucceedWhenArticleWasNeverDistributed() {
        //given
        final FakeDistributionPlatformAdapter adapter = new FakeDistributionPlatformAdapter();

        //when
        final Either<Error, Void> firstResult = adapter.withdraw(ARTICLE.id());
        final Either<Error, Void> secondResult = adapter.withdraw(ARTICLE.id());

        //then
        then(firstResult.isRight()).isTrue();
        then(secondResult.isRight()).isTrue();
        then(adapter.hasDistributed(ARTICLE.id())).isFalse();
    }

}
