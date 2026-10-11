package com.manning.writeflow.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Coordinates the use cases that complete a Publication.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@WorkflowInterface
public interface PublicationWorkflow {

    /**
     * Runs the Publication for an Article.
     *
     * @param articleId the Article identifier
     */
    @WorkflowMethod
    void publish(final String articleId);

}
