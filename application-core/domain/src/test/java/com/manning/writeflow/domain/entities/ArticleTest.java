package com.manning.writeflow.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.DomainEvent;
import com.manning.writeflow.shared.event.PublicationStarted;

import io.vavr.control.Either;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link Article}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("Article tests")
class ArticleTest {

    private static ArticleId articleId() {
        return ArticleId.validateThenCreate(UUID.randomUUID().toString()).get();
    }

    private static AuthorId authorId() {
        return AuthorId.validateThenCreate(UUID.randomUUID().toString()).get();
    }

    private static Title title() {
        return Title.validateThenCreate("A Title").get();
    }

    private static Content content() {
        return Content.validateThenCreate("Some content").get();
    }

    @Test
    @DisplayName("When all fields are valid, then article creation should return a draft")
    void validateThenCreateShouldCreateDraftArticleWhenAllFieldsAreValid() {
        //given
        final ArticleId id = articleId();
        final Title articleTitle = title();
        final Content articleContent = content();
        final AuthorId author = authorId();

        //when
        final Either<Error, Article> result =
            Article.validateThenCreate(id, articleTitle, articleContent, author);

        //then
        then(result.isRight()).isTrue();
        then(result.get().status()).isEqualTo(ArticleStatus.DRAFT);
        then(result.get().popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When all fields are null, then article creation should return validation errors")
    void validateThenCreateShouldReturnValidationErrorsWhenAllFieldsAreNull() {
        //given

        //when
        final Either<Error, Article> result = Article.validateThenCreate(null, null, null, null);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(4);
    }

    @Test
    @DisplayName("When a field is null, then article creation should return a validation error")
    void validateThenCreateShouldReturnValidationErrorsWhenAFieldIsNull() {
        //given
        final ArticleId id = articleId();
        final Content articleContent = content();
        final AuthorId author = authorId();

        //when
        final Either<Error, Article> result =
            Article.validateThenCreate(id, null, articleContent, author);

        //then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    @DisplayName("When the owning author starts publication, then the article should enter PUBLISHING")
    void startPublicationShouldEnterPublishingWhenRequestedByOwningAuthor() {
        //given
        final AuthorId author = authorId();
        final Article article =
            Article.validateThenCreate(articleId(), title(), content(), author).get();

        //when
        final Either<Error, Article> result = article.startPublication(author);

        //then
        then(result.isRight()).isTrue();
        then(result.get().status()).isEqualTo(ArticleStatus.PUBLISHING);
    }

    @Test
    @DisplayName("When publication starts, then the article should raise PublicationStarted")
    void startPublicationShouldRaisePublicationStartedEventWhenPublicationStarts() {
        //given
        final AuthorId author = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), author).get();

        //when
        article.startPublication(author);
        final List<DomainEvent> events = article.popDomainEvents();

        //then
        then(events).hasSize(1);
        then(events.get(0)).isInstanceOf(PublicationStarted.class);
        final PublicationStarted started = (PublicationStarted) events.getFirst();
        then(started.articleId()).isEqualTo(id.value());
        then(started.authorId()).isEqualTo(author.value());
    }

    @Test
    @DisplayName("When domain events are popped, then the next pop should be empty")
    void popDomainEventsShouldClearEventsWhenEventsArePopped() {
        //given
        final AuthorId author = authorId();
        final Article article =
            Article.validateThenCreate(articleId(), title(), content(), author).get();
        article.startPublication(author);

        //when
        final List<DomainEvent> first = article.popDomainEvents();
        final List<DomainEvent> second = article.popDomainEvents();

        //then
        then(first).hasSize(1);
        then(second).isEmpty();
    }

    @Test
    @DisplayName("When publication is already in progress, then a repeated start should be rejected")
    void startPublicationShouldRejectRepeatWhenPublicationIsInProgress() {
        //given
        final AuthorId author = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), author).get();
        article.startPublication(author);
        article.popDomainEvents();

        //when
        final Either<Error, Article> result = article.startPublication(author);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.PublicationInProgress.class);
        final Error.BusinessError.PublicationInProgress error =
            (Error.BusinessError.PublicationInProgress) result.getLeft();
        then(error.articleId()).isEqualTo(id.value());
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHING);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When a non-owner starts publication, then the article should remain a draft")
    void startPublicationShouldRejectRequestWhenAuthorDoesNotOwnArticle() {
        //given
        final AuthorId owner = authorId();
        final AuthorId intruder = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), owner).get();

        //when
        final Either<Error, Article> result = article.startPublication(intruder);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.AuthorNotAllowedToPublish.class);
        final Error.BusinessError.AuthorNotAllowedToPublish error =
            (Error.BusinessError.AuthorNotAllowedToPublish) result.getLeft();
        then(error.authorId()).isEqualTo(intruder.value());
        then(error.articleId()).isEqualTo(id.value());
        then(article.status()).isEqualTo(ArticleStatus.DRAFT);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When an article is reconstituted, then it should preserve state without events")
    void reconstituteShouldPreserveStateWithoutRaisingEvents() {
        //given
        final ArticleId id = articleId();

        //when
        final Article article =
            Article.reconstitute(id, title(), content(), authorId(), ArticleStatus.PUBLISHED);

        //then
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.id()).isEqualTo(id);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When publication is in progress, then confirmation should publish the article")
    void confirmPublicationShouldPublishArticleWhenPublicationIsInProgress() {
        //given
        final Article article = Article.validateThenCreate(articleId(), title(), content(), authorId()).get();
        article.startPublication(article.authorId());
        article.popDomainEvents();

        //when
        final Either<Error, Article> result = article.confirmPublication(Instant.parse("2026-06-07T10:15:30Z"));

        //then
        then(result.isRight()).isTrue();
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.popDomainEvents()).hasSize(1).first().isInstanceOf(ArticlePublished.class);
    }

    @Test
    @DisplayName("When publication is confirmed again, then it should succeed without raising events")
    void confirmPublicationShouldBeIdempotentWhenArticleIsPublished() {
        //given
        final Article article = Article.reconstitute(articleId(), title(), content(), authorId(), ArticleStatus.PUBLISHED);

        //when
        final Either<Error, Article> result = article.confirmPublication(Instant.now());

        //then
        then(result.isRight()).isTrue();
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When publication has not started, then confirmation should be rejected")
    void confirmPublicationShouldRejectRequestWhenArticleIsDraft() {
        //given
        final Article article = Article.validateThenCreate(articleId(), title(), content(), authorId()).get();

        //when
        final Either<Error, Article> result = article.confirmPublication(Instant.now());

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.PublicationNotStarted.class);
        then(article.status()).isEqualTo(ArticleStatus.DRAFT);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When publication is reverted, then repeating the revert should succeed without events")
    void revertPublicationShouldBeIdempotentWhenPublicationIsReverted() {
        //given
        final Article article = Article.validateThenCreate(articleId(), title(), content(), authorId()).get();
        article.startPublication(article.authorId());
        article.popDomainEvents();

        //when
        final Either<Error, Article> result = article.revertPublication();
        final Either<Error, Article> repeated = article.revertPublication();

        //then
        then(result.isRight()).isTrue();
        then(repeated.isRight()).isTrue();
        then(article.status()).isEqualTo(ArticleStatus.DRAFT);
        then(article.popDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("When an article is published, then starting and reverting should be rejected")
    void startAndRevertPublicationShouldRejectRequestWhenArticleIsPublished() {
        //given
        final Article article = Article.reconstitute(articleId(), title(), content(), authorId(), ArticleStatus.PUBLISHED);

        //when
        final Either<Error, Article> start = article.startPublication(article.authorId());
        final Either<Error, Article> revert = article.revertPublication();

        //then
        then(start.getLeft()).isInstanceOf(Error.BusinessError.ArticleAlreadyPublished.class);
        then(revert.getLeft()).isInstanceOf(Error.BusinessError.ArticleAlreadyPublished.class);
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.popDomainEvents()).isEmpty();
    }

}
