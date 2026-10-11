package com.manning.writeflow.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence representation of an article.
 *
 * <p>This is a plain persistence model living in the persistence adapter. It mirrors the
 * shape of the domain {@code Article} aggregate flattened into primitive {@code String}
 * columns. It is mapped to and from the domain entity by {@link ArticlePersistenceMapper}
 * and must never leak across the application boundary.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class JpaArticle {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, length = 10_000)
    private String content;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Convenience constructor used by the mapper to build a fully-populated entity.
     *
     * @param id       the article identifier
     * @param title    the article title
     * @param content  the article content
     * @param authorId the identifier of the author
     * @param status   the lifecycle status of the article
     */
    public JpaArticle(final String id, final String title, final String content, final String authorId,
            final String status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.status = status;
    }

}
