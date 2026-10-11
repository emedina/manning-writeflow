package com.manning.writeflow.api;

/**
 * API request types.
 *
 * <p>HTTP request objects are exempt from the always-valid rule: they are plain Java records
 * with no {@code validateThenCreate} factory. Validation happens when the corresponding
 * command is created.</p>
 *
 * @author Enrique Medina Montenegro
 */
sealed interface ApiRequest {

    /**
     * Body of a request to publish an article.
     *
     * @param authorId the raw identifier of the author requesting publication
     */
    record PublishArticle(String authorId) implements ApiRequest {
    }

    /**
     * Body of a request to create a new draft article.
     *
     * <p>The article identifier is not supplied by the client: the controller generates it
     * server-side so that creation stays simple and the returned identifier can later be reused
     * to publish and retrieve the article.</p>
     *
     * @param authorId the raw identifier of the author creating the article
     * @param title the article title
     * @param content the article content
     */
    record CreateArticle(String authorId, String title, String content) implements ApiRequest {
    }

}
