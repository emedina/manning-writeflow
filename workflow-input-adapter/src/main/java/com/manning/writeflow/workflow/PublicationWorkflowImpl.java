package com.manning.writeflow.workflow;

import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

/**
 * Runs and compensates the steps of a Publication.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@WorkflowImpl(workers = "publication")
public final class PublicationWorkflowImpl implements PublicationWorkflow {

    private final PublicationActivities activities = Workflow.newActivityStub(PublicationActivities.class);

    /**
     * Creates a workflow implementation for Temporal's worker discovery.
     */
    public PublicationWorkflowImpl() {
    }

    @Override
    public void publish(final String articleId) {
        final Saga saga = new Saga(new Saga.Options.Builder().setContinueWithError(true).build());
        saga.addCompensation(this.activities::revertPublication, articleId);
        saga.addCompensation(this.activities::withdrawArticle, articleId);

        try {
            this.activities.distributeArticle(articleId);
            this.activities.confirmPublication(articleId);
        } catch (final ActivityFailure activityFailure) {
            saga.compensate();
            throw activityFailure;
        }
    }

}
