package com.manning.writeflow.domain.entities;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.DomainEvent;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link Article}.
 *
 * @author Enrique Medina Montenegro
 */
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
    void shouldCreateDraftArticle_whenAllFieldsValid() {
        // when
        final Either<Error, Article> result =
            Article.validateThenCreate(articleId(), title(), content(), authorId());

        // then
        then(result.isRight()).isTrue();
        then(result.get().status()).isEqualTo(ArticleStatus.DRAFT);
        then(result.get().popDomainEvents()).isEmpty();
    }

    @Test
    void shouldReturnValidationErrors_whenAllFieldsNull() {
        // when
        final Either<Error, Article> result = Article.validateThenCreate(null, null, null, null);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.ValidationErrors.class);
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(4);
    }

    @Test
    void shouldReturnValidationErrors_whenSingleFieldNull() {
        // when
        final Either<Error, Article> result =
            Article.validateThenCreate(articleId(), null, content(), authorId());

        // then
        then(result.isLeft()).isTrue();
        final Error.ValidationErrors errors = (Error.ValidationErrors) result.getLeft();
        then(errors.errors()).hasSize(1);
    }

    @Test
    void shouldPublishArticle_whenDraftAndRequestedByOwningAuthor() {
        // given
        final AuthorId author = authorId();
        final Article article =
            Article.validateThenCreate(articleId(), title(), content(), author).get();

        // when
        final Either<Error, Article> result = article.publish(author);

        // then
        then(result.isRight()).isTrue();
        then(result.get().status()).isEqualTo(ArticleStatus.PUBLISHED);
    }

    @Test
    void shouldRaiseArticlePublishedEvent_whenPublished() {
        // given
        final AuthorId author = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), author).get();

        // when
        article.publish(author);
        final List<DomainEvent> events = article.popDomainEvents();

        // then
        then(events).hasSize(1);
        then(events.get(0)).isInstanceOf(ArticlePublished.class);
        final ArticlePublished published = (ArticlePublished) events.get(0);
        then(published.articleId()).isEqualTo(id.value());
        then(published.authorId()).isEqualTo(author.value());
        then(published.publishedAt()).isNotNull();
    }

    @Test
    void shouldClearDomainEvents_whenPopped() {
        // given
        final AuthorId author = authorId();
        final Article article =
            Article.validateThenCreate(articleId(), title(), content(), author).get();
        article.publish(author);

        // when
        final List<DomainEvent> first = article.popDomainEvents();
        final List<DomainEvent> second = article.popDomainEvents();

        // then
        then(first).hasSize(1);
        then(second).isEmpty();
    }

    @Test
    void shouldRejectPublication_whenArticleAlreadyPublished() {
        // given
        final AuthorId author = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), author).get();
        article.publish(author);
        article.popDomainEvents();

        // when
        final Either<Error, Article> result = article.publish(author);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.ArticleAlreadyPublished.class);
        final Error.BusinessError.ArticleAlreadyPublished error =
            (Error.BusinessError.ArticleAlreadyPublished) result.getLeft();
        then(error.articleId()).isEqualTo(id.value());
    }

    @Test
    void shouldRejectPublication_whenRequestingAuthorIsNotTheOwner() {
        // given
        final AuthorId owner = authorId();
        final AuthorId intruder = authorId();
        final ArticleId id = articleId();
        final Article article = Article.validateThenCreate(id, title(), content(), owner).get();

        // when
        final Either<Error, Article> result = article.publish(intruder);

        // then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.AuthorNotAllowedToPublish.class);
        final Error.BusinessError.AuthorNotAllowedToPublish error =
            (Error.BusinessError.AuthorNotAllowedToPublish) result.getLeft();
        then(error.authorId()).isEqualTo(intruder.value());
        then(error.articleId()).isEqualTo(id.value());
        then(article.status()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void shouldReconstituteArticle_withoutValidationAndEmptyEvents() {
        // given
        final ArticleId id = articleId();

        // when
        final Article article =
            Article.reconstitute(id, title(), content(), authorId(), ArticleStatus.PUBLISHED);

        // then
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(article.id()).isEqualTo(id);
        then(article.popDomainEvents()).isEmpty();
    }

}
