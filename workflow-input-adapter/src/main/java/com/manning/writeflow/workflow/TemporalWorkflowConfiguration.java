package com.manning.writeflow.workflow;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImplementationOptionsCustomizer;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;

/**
 * Configures Temporal activity deadlines and retry behavior.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@AutoConfiguration
public final class TemporalWorkflowConfiguration {

    private static final Duration ACTIVITY_START_TO_CLOSE_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration RETRY_INITIAL_INTERVAL = Duration.ofSeconds(1);

    @Bean
    WorkflowImplementationOptionsCustomizer publicationActivityOptionsCustomizer(
        @Value("${writeflow.publication.distribution-schedule-to-close:PT24H}") final Duration distributionDeadline) {
        final ActivityOptions distributionOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
            .setScheduleToCloseTimeout(distributionDeadline)
            .setRetryOptions(this.retryOptions())
            .build();
        final ActivityOptions attemptOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
            .setRetryOptions(this.retryOptions())
            .build();

        return (final WorkflowImplementationOptions.Builder options, final io.temporal.worker.Worker worker,
            final Class<?> workflowImplementationClass,
            final io.temporal.common.metadata.POJOWorkflowMethodMetadata workflowMethodMetadata) -> {
                if (workflowImplementationClass == PublicationWorkflowImpl.class) {
                    return options.setActivityOptions(Map.of(
                        "distributeArticle", distributionOptions,
                        "confirmPublication", attemptOptions,
                        "revertPublication", attemptOptions,
                        "withdrawArticle", attemptOptions));
                }
                return options;
            };
    }

    private RetryOptions retryOptions() {
        return RetryOptions.newBuilder()
            .setInitialInterval(RETRY_INITIAL_INTERVAL)
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(0)
            .build();
    }

    @Bean
    TemporalWorkerStartup temporalWorkerStartup(final WorkerFactory workerFactory) {
        return new TemporalWorkerStartup(workerFactory);
    }

}
