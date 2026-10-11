package com.manning.writeflow.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Exposes the Publication use cases as Temporal activities.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ActivityInterface
public interface PublicationActivities {

    /**
     * Distributes an Article.
     *
     * @param articleId the Article identifier
     */
    @ActivityMethod
    void distributeArticle(final String articleId);

    /**
     * Confirms an Article publication.
     *
     * @param articleId the Article identifier
     */
    @ActivityMethod
    void confirmPublication(final String articleId);

    /**
     * Reverts an Article to Draft.
     *
     * @param articleId the Article identifier
     */
    @ActivityMethod
    void revertPublication(final String articleId);

    /**
     * Withdraws an Article from the Distribution Platform.
     *
     * @param articleId the Article identifier
     */
    @ActivityMethod
    void withdrawArticle(final String articleId);

}
