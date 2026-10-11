package com.manning.writeflow.shared.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Article content passed to the Distribution Platform.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
public record ArticleForDistributionDTO(String id, String title, String content, String authorId)
    implements Serializable {

    public ArticleForDistributionDTO {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
    }

}
