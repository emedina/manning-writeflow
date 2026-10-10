package com.manning.writeflow.domain.entities;

import com.manning.writeflow.shared.error.Error;
import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.DomainEvent;
import com.manning.writeflow.shared.validation.ValidationError;
import com.manning.writeflow.shared.validation.Validations;
import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an article written by an author in our Domain Model.
 *
 * <p>This is the aggregate root of the WriteFlow domain. It is always-valid: the only
 * way to obtain a fresh instance is through {@link #validateThenCreate(ArticleId, Title, Content, AuthorId)}, which is
 * Either-only and accumulates field errors. Persistence rehydration uses
 * {@link #reconstitute(ArticleId, Title, Content, AuthorId, ArticleStatus)}, which performs no validation. Domain
 * events raised by behaviour are collected internally and drained via {@link #popDomainEvents()}.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Accessors(fluent = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Article {

    @Getter
    @EqualsAndHashCode.Include
    private final ArticleId id;

    @Getter
    private final Title title;
    @Getter
    private final Content content;
    @Getter
    private final AuthorId authorId;
    @Getter
    private ArticleStatus status;

    // No getter to avoid direct manipulation of the collection.
    private final List<DomainEvent> domainEvents;

    /**
     * Validates the mandatory fields of an article and creates a new instance in {@link ArticleStatus#DRAFT}.
     *
     * @param id       the identifier of the article
     * @param title    the title of the article
     * @param content  the content of the article
     * @param authorId the identifier of the author of the article
     * @return {@link Either#right} with the new article when all mandatory fields are present, otherwise
     * {@link Either#left} with the accumulated {@link Error.ValidationErrors}
     */
    public static Either<Error, Article> validateThenCreate(
        final ArticleId id, final Title title,
        final Content content, final AuthorId authorId
    ) {
        final List<ValidationError> errors = new ArrayList<>();
        Validations.mandatory(id, "id").peekLeft(errors::add);
        Validations.mandatory(title, "title").peekLeft(errors::add);
        Validations.mandatory(content, "content").peekLeft(errors::add);
        Validations.mandatory(authorId, "authorId").peekLeft(errors::add);

        return errors.isEmpty()
            ? Either.right(new Article(id, title, content, authorId, ArticleStatus.DRAFT, new ArrayList<>()))
            : Either.left(new Error.ValidationErrors(List.copyOf(errors)));
    }

    /**
     * Reconstitutes an article from its already-validated persisted state.
     *
     * <p>This is the domain's <em>reconstitution</em> path, deliberately distinct from the
     * <em>creation</em> path ({@link #validateThenCreate(ArticleId, Title, Content, AuthorId)}).
     * The distinction is reconstitution-vs-creation, not domain-vs-infrastructure: rebuilding an aggregate that already
     * existed is a legitimate domain concern, so this factory lives on the aggregate itself and takes only domain types
     * &mdash; it imports nothing from any persistence technology. Per the established DDD idiom (Vernon's
     * reconstitution constructor), the reconstitution path accepts the persisted {@link ArticleStatus} as ground truth
     * and therefore performs <strong>no validation</strong>, raises <strong>no domain events</strong>, and never fails.
     * Creation-time invariants and the {@code ArticlePublished} event belong to the creation and
     * {@link #publish(AuthorId, Instant)} paths only, never to a load from storage.</p>
     *
     * @param id       the identifier of the article
     * @param title    the title of the article
     * @param content  the content of the article
     * @param authorId the identifier of the author of the article
     * @param status   the persisted lifecycle status of the article, taken as ground truth
     * @return a reconstituted article with an empty domain-event list
     */
    public static Article reconstitute(
        final ArticleId id, final Title title, final Content content,
        final AuthorId authorId, final ArticleStatus status
    ) {
        return new Article(id, title, content, authorId, status, new ArrayList<>());
    }

    /**
     * Publishes this article on behalf of the requesting author.
     *
     * @param requestingAuthor the author requesting publication
     * @param publicationDate  the date of publication
     * @return {@link Either#right} with this article transitioned to {@link ArticleStatus#PUBLISHED} when allowed,
     * otherwise {@link Either#left} with the appropriate {@link Error.BusinessError}
     */
    public Either<Error, Article> publish(final AuthorId requestingAuthor, final Instant publicationDate) {
        if (this.status != ArticleStatus.DRAFT) {
            return Either.left(new Error.BusinessError.ArticleAlreadyPublished(this.id.value()));
        }
        if (!this.authorId.equals(requestingAuthor)) {
            return Either.left(new Error.BusinessError.AuthorNotAllowedToPublish(
                requestingAuthor.value(), this.id.value()));
        }
        this.status = ArticleStatus.PUBLISHED;
        this.domainEvents.add(new ArticlePublished(this.id.value(), this.authorId.value(), publicationDate));

        return Either.right(this);
    }

    /**
     * Drains and returns the domain events accumulated by this article.
     *
     * @return an immutable copy of the pending domain events; the internal buffer is cleared
     */
    public List<DomainEvent> popDomainEvents() {
        final List<DomainEvent> popped = List.copyOf(this.domainEvents);
        this.domainEvents.clear();

        return popped;
    }

}
