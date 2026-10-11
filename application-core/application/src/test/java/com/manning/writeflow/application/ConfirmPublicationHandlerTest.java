package com.manning.writeflow.application;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manning.writeflow.application.command.ConfirmPublicationCommand;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import io.vavr.control.Either;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConfirmPublicationHandler}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class ConfirmPublicationHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String TITLE = "A perfectly valid title";
    private static final String CONTENT = "Some perfectly valid content";
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Mock private ArticleRepository articleRepository;
    @Mock private DomainEventPublisherOutputPort domainEventPublisher;
    @InjectMocks private ConfirmPublicationHandler handler;

    @Test
    @DisplayName("handle should complete the use case")
    void handleShouldCompleteUseCase() {
        //given
        final Article article = article(ArticleStatus.PUBLISHING);
        article.popDomainEvents();
        final ArticlePublished expected = new ArticlePublished(ARTICLE_ID, AUTHOR_ID, PUBLISHED_AT);
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.right(article));
        given(this.articleRepository.save(article)).willReturn(Either.right(null));
        given(this.domainEventPublisher.publish(List.of(expected))).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, PUBLISHED_AT).get());

        //then
        then(result.isRight()).isTrue();
        then(article.status()).isEqualTo(ArticleStatus.PUBLISHED);
        verify(this.domainEventPublisher).publish(List.of(expected));
    }

    @Test
    @DisplayName("handle should publish no events when publication is already confirmed")
    void handleShouldPublishNoEventsWhenPublicationIsAlreadyConfirmed() {
        //given
        final Article article = article(ArticleStatus.PUBLISHED);
        article.popDomainEvents();
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get())).willReturn(Either.right(article));
        given(this.articleRepository.save(article)).willReturn(Either.right(null));
        given(this.domainEventPublisher.publish(List.of())).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, PUBLISHED_AT).get());

        //then
        then(result.isRight()).isTrue();
        verify(this.domainEventPublisher).publish(List.of());
    }

    @Test
    @DisplayName("handle should not publish events when save fails")
    void handleShouldNotPublishEventsWhenSaveFails() {
        //given
        final Article article = article(ArticleStatus.PUBLISHING);
        article.popDomainEvents();
        final Error error = new Error.TechnicalError.PersistenceError("repo", "save", "failed");
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get()))
            .willReturn(Either.right(article));
        given(this.articleRepository.save(article)).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(
            ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, PUBLISHED_AT).get());

        //then
        then(result.getLeft()).isEqualTo(error);
        verify(this.domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("handle should stop when publication has not started")
    void handleShouldStopWhenPublicationHasNotStarted() {
        //given
        final Article article = article(ArticleStatus.DRAFT);
        given(this.articleRepository.readById(ArticleId.validateThenCreate(ARTICLE_ID).get()))
            .willReturn(Either.right(article));

        //when
        final Either<Error, Void> result = this.handler.handle(
            ConfirmPublicationCommand.validateThenCreate(ARTICLE_ID, PUBLISHED_AT).get());

        //then
        then(result.getLeft()).isInstanceOf(Error.BusinessError.PublicationNotStarted.class);
        verify(this.articleRepository, never()).save(any());
        verify(this.domainEventPublisher, never()).publish(any());
    }

    private static Article article(final ArticleStatus status) {
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Title title = Title.validateThenCreate(TITLE).get();
        final Content content = Content.validateThenCreate(CONTENT).get();
        final Article article = Article.validateThenCreate(articleId, title, content, authorId).get();
        if (status == ArticleStatus.PUBLISHING) {
            article.startPublication(authorId);
        }
        if (status == ArticleStatus.PUBLISHED) {
            article.startPublication(authorId);
            article.confirmPublication(PUBLISHED_AT);
        }
        return article;
    }

}
