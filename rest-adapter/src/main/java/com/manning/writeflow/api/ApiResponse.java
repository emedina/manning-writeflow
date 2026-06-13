package com.manning.writeflow.api;

/**
 * API response types.
 *
 * <p>HTTP response objects are exempt from the always-valid rule: they are plain Java records
 * with no {@code validateThenCreate} factory.</p>
 *
 * @author Enrique Medina Montenegro
 */
sealed interface ApiResponse {

    /**
     * Representation of an article returned to the client.
     *
     * @param id the article identifier
     * @param title the article title
     * @param authorId the identifier of the author
     * @param status the article status
     */
    record Article(String id, String title, String authorId, String status) implements ApiResponse {
    }

}
