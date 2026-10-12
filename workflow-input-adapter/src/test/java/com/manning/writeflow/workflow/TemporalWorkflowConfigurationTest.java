package com.manning.writeflow.workflow;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImplementationOptionsCustomizer;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * Tests the Temporal policy supplied by the workflow adapter's Spring configuration.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class TemporalWorkflowConfigurationTest {

    private TemporalWorkflowConfiguration subject;

    @BeforeEach
    void setUp() {
        this.subject = new TemporalWorkflowConfiguration();
    }

    @Test
    @DisplayName("publication policy should configure distribution deadline and unlimited retries")
    void publicationPolicyShouldConfigureDistributionDeadlineAndUnlimitedRetries() {
        //given
        final Duration distributionDeadline = Duration.ofHours(8);
        final Worker worker = mock(Worker.class);
        final WorkflowImplementationOptionsCustomizer customizer =
            this.subject.publicationActivityOptionsCustomizer(distributionDeadline);
        final WorkflowImplementationOptions.Builder builder = WorkflowImplementationOptions.newBuilder();

        //when
        final WorkflowImplementationOptions options = customizer
            .customize(builder, worker, PublicationWorkflowImpl.class, null)
            .build();

        //then
        final Map<String, ActivityOptions> activityOptions = options.getActivityOptions();
        final ActivityOptions distribution = activityOptions.get("DistributeArticle");
        then(activityOptions).containsOnlyKeys(
            "DistributeArticle", "ConfirmPublication", "RevertPublication", "WithdrawArticle");
        then(distribution.getScheduleToCloseTimeout()).isEqualTo(distributionDeadline);
        then(distribution.getStartToCloseTimeout()).isEqualTo(Duration.ofMinutes(1));
        then(distribution.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(1));
        then(distribution.getRetryOptions().getBackoffCoefficient()).isEqualTo(2.0);
        then(distribution.getRetryOptions().getMaximumAttempts()).isZero();
        then(activityOptions.get("ConfirmPublication").getScheduleToCloseTimeout()).isNull();
        then(activityOptions.get("RevertPublication").getScheduleToCloseTimeout()).isNull();
        then(activityOptions.get("WithdrawArticle").getScheduleToCloseTimeout()).isNull();
        then(activityOptions.values())
            .allSatisfy((final ActivityOptions activity) ->
                then(activity.getStartToCloseTimeout()).isEqualTo(Duration.ofMinutes(1)));
        then(activityOptions.values())
            .allSatisfy((final ActivityOptions activity) ->
                then(activity.getRetryOptions().getMaximumAttempts()).isZero());
        then(activityOptions.values())
            .allSatisfy((final ActivityOptions activity) ->
                then(activity.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(1)));
        then(activityOptions.values())
            .allSatisfy((final ActivityOptions activity) ->
                then(activity.getRetryOptions().getBackoffCoefficient()).isEqualTo(2.0));
    }

    @Test
    @DisplayName("customizer should leave options unchanged for a different workflow")
    void customizerShouldLeaveOptionsUnchangedForADifferentWorkflow() {
        //given
        final Worker worker = mock(Worker.class);
        final WorkflowImplementationOptionsCustomizer customizer =
            this.subject.publicationActivityOptionsCustomizer(Duration.ofHours(8));
        final WorkflowImplementationOptions.Builder builder = WorkflowImplementationOptions.newBuilder();

        //when
        final WorkflowImplementationOptions options = customizer
            .customize(builder, worker, String.class, null)
            .build();

        //then
        then(options.getActivityOptions()).isEmpty();
    }

    @Test
    @DisplayName("bean factory method should create worker startup lifecycle")
    void beanFactoryMethodShouldCreateWorkerStartupLifecycle() {
        //given
        final WorkerFactory workerFactory = mock(WorkerFactory.class);

        //when
        final TemporalWorkerStartup result = this.subject.temporalWorkerStartup(workerFactory);

        //then
        then(result).isInstanceOf(TemporalWorkerStartup.class);
    }
}
