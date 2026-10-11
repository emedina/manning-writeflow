package com.manning.writeflow.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manning.writeflow.application.command.PublishArticleCommand;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.application.ports.out.DomainEventPublisherOutputPort;
import com.manning.writeflow.domain.entities.Article;
import com.manning.writeflow.domain.entities.ArticleId;
import com.manning.writeflow.domain.entities.ArticleStatus;
import com.manning.writeflow.domain.entities.AuthorId;
import com.manning.writeflow.domain.entities.Content;
import com.manning.writeflow.domain.entities.Title;
import com.manning.writeflow.domain.repositories.ArticleRepository;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PublishArticleHandler}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
class PublishArticleHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String AUTHOR_ID = "22222222-2222-2222-2222-222222222222";
    private static final String OTHER_AUTHOR_ID = "33333333-3333-3333-3333-333333333333";

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private DomainEventPublisherOutputPort domainEventPublisher;

    @Mock
    private DistributionPlatformOutputPort distributionPlatform;

    @Mock
    private PublishArticleCommand command;

    @InjectMocks
    private PublishArticleHandler handler;

    @Test
    @DisplayName("handle should publish the article when the draft belongs to the requesting author")
    void handleShouldPublishArticleWhenDraftBelongsToRequestingAuthor() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Article draft = draftArticle(articleId, authorId);

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publicationDate()).willReturn(Instant.now());
        given(this.articleRepository.readById(articleId)).willReturn(Either.right(draft));
        given(this.articleRepository.save(draft)).willReturn(Either.right(null));
        given(this.distributionPlatform.distribute(org.mockito.ArgumentMatchers.any()))
            .willReturn(Either.right(null));
        given(this.domainEventPublisher.publish(org.mockito.ArgumentMatchers.anyList())).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isRight()).isTrue();
        verify(this.articleRepository).save(draft);
        verify(this.distributionPlatform).distribute(org.mockito.ArgumentMatchers.any());
        verify(this.domainEventPublisher).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("handle should return not found when the article does not exist")
    void handleShouldReturnNotFoundWhenArticleDoesNotExist() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final Error.BusinessError.NotFound notFound =
            new Error.BusinessError.NotFound("JpaArticleRepository", "Article", ARTICLE_ID);

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.articleRepository.readById(articleId)).willReturn(Either.left(notFound));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(notFound);
        verify(this.articleRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(this.distributionPlatform, never()).distribute(org.mockito.ArgumentMatchers.any());
        verify(this.domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("handle should return already published when the article is not a draft")
    void handleShouldReturnAlreadyPublishedWhenArticleIsNotDraft() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Article published = draftArticle(articleId, authorId);
        published.publish(authorId, Instant.now());

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publicationDate()).willReturn(Instant.now());
        given(this.articleRepository.readById(articleId)).willReturn(Either.right(published));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.ArticleAlreadyPublished.class);
        verify(this.articleRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(this.distributionPlatform, never()).distribute(org.mockito.ArgumentMatchers.any());
        verify(this.domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("handle should reject publishing when the requesting author is not the owner")
    void handleShouldRejectPublishingWhenRequestingAuthorIsNotOwner() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId owner = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Article draft = draftArticle(articleId, owner);

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(OTHER_AUTHOR_ID);
        given(this.command.publicationDate()).willReturn(Instant.now());
        given(this.articleRepository.readById(articleId)).willReturn(Either.right(draft));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isInstanceOf(Error.BusinessError.AuthorNotAllowedToPublish.class);
        verify(this.articleRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(this.distributionPlatform, never()).distribute(org.mockito.ArgumentMatchers.any());
        verify(this.domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("handle should leave an orphan at the platform when saving fails after distribution")
    void handleShouldLeaveOrphanAtPlatformWhenSaveFailsAfterDistribution() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Article draft = draftArticle(articleId, authorId);
        final Error.TechnicalError.PersistenceError persistenceError =
            new Error.TechnicalError.PersistenceError("JpaArticleRepository", "save", "boom");
        final Set<String> distributedArticleIds = new HashSet<>();
        final ArticleDTO persistedDraft = new ArticleDTO(
            ARTICLE_ID, "A perfectly valid title", AUTHOR_ID, ArticleStatus.DRAFT.name());
        final AtomicReference<ArticleDTO> databaseArticle = new AtomicReference<>(persistedDraft);

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publicationDate()).willReturn(Instant.now());
        given(this.articleRepository.readById(articleId)).willReturn(Either.right(draft));
        given(this.distributionPlatform.distribute(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
            final ArticleForDistributionDTO distributedArticle = invocation.getArgument(0);
            distributedArticleIds.add(distributedArticle.id());
            return Either.right(null);
        });
        given(this.articleRepository.save(draft)).willAnswer(invocation -> {
            final Article articleToSave = invocation.getArgument(0);
            final Either<Error, Void> saveResult = Either.left(persistenceError);
            return saveResult.peek(_ -> databaseArticle.set(toDTO(articleToSave)));
        });
        given(this.articleRepository.findById(articleId)).willAnswer(_ -> Either.right(databaseArticle.get()));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(persistenceError);
        then(distributedArticleIds).contains(ARTICLE_ID);
        then(draft.status()).isEqualTo(ArticleStatus.PUBLISHED);
        then(this.articleRepository.findById(articleId).get().status()).isEqualTo(ArticleStatus.DRAFT.name());
        final InOrder interactionOrder = inOrder(this.distributionPlatform, this.articleRepository);
        interactionOrder.verify(this.distributionPlatform).distribute(org.mockito.ArgumentMatchers.argThat(article ->
            article.id().equals(ARTICLE_ID)));
        interactionOrder.verify(this.articleRepository).save(draft);
        verify(this.domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("handle should return the publisher error when publishing domain events fails")
    void handleShouldReturnPublisherErrorWhenPublishingDomainEventsFails() {
        //given
        final ArticleId articleId = ArticleId.validateThenCreate(ARTICLE_ID).get();
        final AuthorId authorId = AuthorId.validateThenCreate(AUTHOR_ID).get();
        final Article draft = draftArticle(articleId, authorId);
        final Error.TechnicalError.SomethingWentWrong publisherError =
            new Error.TechnicalError.SomethingWentWrong("SpringDomainEventPublisher", "dispatch failed");

        given(this.command.articleId()).willReturn(ARTICLE_ID);
        given(this.command.authorId()).willReturn(AUTHOR_ID);
        given(this.command.publicationDate()).willReturn(Instant.now());
        given(this.articleRepository.readById(articleId)).willReturn(Either.right(draft));
        given(this.distributionPlatform.distribute(org.mockito.ArgumentMatchers.any()))
            .willReturn(Either.right(null));
        given(this.articleRepository.save(draft)).willReturn(Either.right(null));
        given(this.domainEventPublisher.publish(org.mockito.ArgumentMatchers.anyList()))
            .willReturn(Either.left(publisherError));

        //when
        final Either<Error, Void> result = this.handler.handle(this.command);

        //then
        then(result.isLeft()).isTrue();
        then(result.getLeft()).isEqualTo(publisherError);
        verify(this.articleRepository).save(draft);
        verify(this.distributionPlatform).distribute(org.mockito.ArgumentMatchers.any());
        verify(this.domainEventPublisher).publish(org.mockito.ArgumentMatchers.anyList());
    }

    private static Article draftArticle(final ArticleId articleId, final AuthorId authorId) {
        final Title title = Title.validateThenCreate("A perfectly valid title").get();
        final Content content = Content.validateThenCreate("Some perfectly valid content").get();

        return Article.validateThenCreate(articleId, title, content, authorId).get();
    }

    private static ArticleDTO toDTO(final Article article) {
        return new ArticleDTO(
            article.id().value(),
            article.title().value(),
            article.authorId().value(),
            article.status().name());
    }

}
