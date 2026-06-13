package com.manning.writeflow.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to create the {@link ResponseEntity} based on an {@link ApiResponse} for successful responses
 * or a {@link ProblemDetail} for failure responses.
 *
 * @author Enrique Medina Montenegro
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ApiResultUtils {

    /**
     * Creates a success response with the given status and optional body.
     *
     * @param status the HTTP status
     * @param response the response body (may be {@code null} for empty-body responses)
     * @return the success response
     */
    static ResponseEntity<ApiResponse> createSuccessResponse(final HttpStatusCode status,
                                                             final ApiResponse response) {
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Creates a failure response with a list of {@link ProblemDetail}, setting the originating {@link URI}.
     *
     * @param problemDetails the list of problems
     * @param uri the originating URI
     * @return the failure response
     */
    static ResponseEntity<List<ProblemDetail>> createFailureResponse(final List<ProblemDetail> problemDetails,
                                                                     final URI uri) {
        return ResponseEntity
                // If there is only one problem, and it has a status code, return that status code.
                .status(problemDetails.size() == 1
                        ? HttpStatusCode.valueOf(problemDetails.get(0).getStatus())
                        : HttpStatus.I_AM_A_TEAPOT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(problemDetails.stream().map(detail -> {
                    detail.setType(uri);
                    return detail;
                }).toList());
    }

}
