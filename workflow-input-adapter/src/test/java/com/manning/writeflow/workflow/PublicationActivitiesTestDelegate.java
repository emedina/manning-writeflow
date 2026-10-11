package com.manning.writeflow.workflow;

/**
 * Gives Temporal a concrete activity implementation while retaining Mockito as the test seam.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
public final class PublicationActivitiesTestDelegate implements PublicationActivities {

    private final PublicationActivities delegate;

    public PublicationActivitiesTestDelegate(final PublicationActivities delegate) {
        this.delegate = delegate;
    }

    @Override
    public void distributeArticle(final String articleId) {
        this.delegate.distributeArticle(articleId);
    }

    @Override
    public void confirmPublication(final String articleId) {
        this.delegate.confirmPublication(articleId);
    }

    @Override
    public void revertPublication(final String articleId) {
        this.delegate.revertPublication(articleId);
    }

    @Override
    public void withdrawArticle(final String articleId) {
        this.delegate.withdrawArticle(articleId);
    }
}
