package com.manning.writeflow.workflow;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;

import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests Publication orchestration with Temporal's in-process service.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class PublicationWorkflowTest {

    private static final String TASK_QUEUE = "publication-test";
    private static final String ARTICLE_ID = "f3f4dd28-9f3d-4cb4-bc9a-3a7ccbc7a700";

    private TestWorkflowEnvironment environment;
    private PublicationActivities activities;
    private PublicationWorkflow workflow;

    @BeforeEach
    void setUp() {
        this.environment = TestWorkflowEnvironment.newInstance();
        final Worker worker = this.environment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setDefaultActivityOptions(ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .build())
                .build(),
            PublicationWorkflowImpl.class);
        this.activities = mock(PublicationActivities.class);
        worker.registerActivitiesImplementations(new PublicationActivitiesTestDelegate(this.activities));
        this.environment.start();
        this.workflow = this.environment.getWorkflowClient().newWorkflowStub(PublicationWorkflow.class,
            io.temporal.client.WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    }

    @AfterEach
    void tearDown() {
        this.environment.close();
    }

    @Test
    @DisplayName("publish should distribute and confirm in order")
    void publishShouldDistributeAndConfirmInOrder() {
        //given
        //when
        this.workflow.publish(ARTICLE_ID);

        //then
        final InOrder order = inOrder(this.activities);
        order.verify(this.activities).distributeArticle(ARTICLE_ID);
        order.verify(this.activities).confirmPublication(ARTICLE_ID);
    }

    @Test
    @DisplayName("publish should retry a transient distribution failure")
    void publishShouldRetryTransientDistributionFailure() {
        //given
        final AtomicInteger attempts = new AtomicInteger();
        willAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw ApplicationFailure.newFailure("temporary", "Transient");
            }
            return null;
        }).given(this.activities).distributeArticle(ARTICLE_ID);

        //when
        this.workflow.publish(ARTICLE_ID);

        //then
        then(attempts.get()).isEqualTo(2);
        verify(this.activities).confirmPublication(ARTICLE_ID);
    }

    @Test
    @DisplayName("publish should withdraw and revert after distribution is rejected")
    void publishShouldWithdrawAndRevertAfterDistributionIsRejected() {
        //given
        willThrow(ApplicationFailure.newNonRetryableFailure("rejected", "Business"))
            .given(this.activities).distributeArticle(ARTICLE_ID);

        //when
        final WorkflowFailedException result = catchThrowableOfType(
            () -> this.workflow.publish(ARTICLE_ID),
            WorkflowFailedException.class);

        //then
        then(result).isNotNull();
        final InOrder order = inOrder(this.activities);
        order.verify(this.activities).withdrawArticle(ARTICLE_ID);
        order.verify(this.activities).revertPublication(ARTICLE_ID);
    }

    @Test
    @DisplayName("publish should withdraw and revert after confirmation is rejected")
    void publishShouldWithdrawAndRevertAfterConfirmationIsRejected() {
        //given
        willThrow(ApplicationFailure.newNonRetryableFailure("cannot confirm", "Business"))
            .given(this.activities).confirmPublication(ARTICLE_ID);

        //when
        final WorkflowFailedException result = catchThrowableOfType(
            () -> this.workflow.publish(ARTICLE_ID),
            WorkflowFailedException.class);

        //then
        then(result).isNotNull();
        final InOrder order = inOrder(this.activities);
        order.verify(this.activities).withdrawArticle(ARTICLE_ID);
        order.verify(this.activities).revertPublication(ARTICLE_ID);
    }

    @Test
    @DisplayName("publish should continue to revert after withdrawal retries succeed")
    void publishShouldContinueToRevertAfterWithdrawalRetriesSucceed() {
        //given
        willThrow(ApplicationFailure.newNonRetryableFailure("rejected", "Business"))
            .given(this.activities).distributeArticle(ARTICLE_ID);
        final AtomicInteger attempts = new AtomicInteger();
        willAnswer(invocation -> {
            if (attempts.getAndIncrement() < 2) {
                throw ApplicationFailure.newFailure("temporary", "Transient");
            }
            return null;
        }).given(this.activities).withdrawArticle(ARTICLE_ID);

        //when
        final WorkflowFailedException result = catchThrowableOfType(
            () -> this.workflow.publish(ARTICLE_ID),
            WorkflowFailedException.class);

        //then
        then(result).isNotNull();
        then(attempts.get()).isEqualTo(3);
        verify(this.activities, times(1)).revertPublication(ARTICLE_ID);
    }
}
