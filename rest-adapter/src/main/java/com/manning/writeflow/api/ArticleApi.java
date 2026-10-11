package com.manning.writeflow.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;

/**
 * API interface declaring the available features as REST endpoints, together with metadata annotations
 * for OpenAPI spec documentation.
 * <p>
 * Notice the usage of the sealed interface feature in order to enhance the control over class inheritance
 * and improve the design of class hierarchies.
 * </p>
 *
 * @author Enrique Medina Montenegro
 */
@RequestMapping(value = "/api/articles", produces = APPLICATION_JSON_VALUE)
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "403", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "409", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(type = "string")))
})
sealed interface ArticleApi permits ArticleController {

    /**
     * Creates a new draft article.
     *
     * @param articleRequest the body carrying the author, title and content of the article to create
     * @param request the originating HTTP request
     * @return a 201 Created response with the created article on success, or a problem details response on failure
     */
    @PostMapping
    @Operation(
            summary = "Creates an article",
            description = "Creates a new draft article"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The article was created successfully"),
            @ApiResponse(responseCode = "400", description = "The provided data is invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> create(final ApiRequest.CreateArticle articleRequest, final HttpServletRequest request);

    /**
     * Publishes an existing draft article.
     *
     * @param articleId the identifier of the article to publish
     * @param articleRequest the body carrying the requesting author
     * @param request the originating HTTP request
     * @return a 202 Accepted response with the article resource location, or a problem details response on failure
     */
    @PostMapping(path = "/{articleId}/publication")
    @Operation(
            summary = "Publishes an article",
            description = "Publishes an existing draft article identified by its identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Publication was accepted"),
            @ApiResponse(responseCode = "400", description = "The provided data is invalid"),
            @ApiResponse(responseCode = "403", description = "The requesting author is not allowed to publish the article"),
            @ApiResponse(responseCode = "404", description = "The identifier is unknown to the system"),
            @ApiResponse(responseCode = "409", description = "The article is already published"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> publish(final String articleId, final ApiRequest.PublishArticle articleRequest,
            final HttpServletRequest request);

    /**
     * Retrieves an article by its identifier.
     *
     * @param articleId the identifier of the article to retrieve
     * @param request the originating HTTP request
     * @return a 200 OK response with the article on success, or a problem details response on failure
     */
    @GetMapping(path = "/{articleId}")
    @Operation(
            summary = "Retrieve an article by its identifier",
            description = "Retrieves an article by its identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "An article with the provided identifier was found"),
            @ApiResponse(responseCode = "400", description = "The provided identifier is invalid"),
            @ApiResponse(responseCode = "404", description = "The identifier is unknown to the system"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> find(final String articleId, final HttpServletRequest request);

}
