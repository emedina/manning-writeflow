package com.manning.writeflow.shared.dto;

import java.io.Serializable;

/**
 * Plain data-transfer representation of an article crossing the application boundary.
 *
 * <p>DTOs are exempt from the always-valid rule: this is a plain Java record with no
 * {@code validateThenCreate} factory.</p>
 *
 * @author Enrique Medina Montenegro
 */
public record ArticleDTO(String id, String title, String authorId, String status) implements Serializable {
}
