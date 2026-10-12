package com.manning.writeflow;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.manning.writeflow.outboxrelay.OutboxRelay;
import com.manning.writeflow.repositories.ArticlePublishedIntegrationEvent;
import com.manning.writeflow.repositories.PublicationStartedIntegrationEvent;
import com.manning.writeflow.workflow.PublicationWorkflow;

import io.temporal.common.metadata.POJOWorkflowInterfaceMetadata;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import static org.assertj.core.api.BDDAssertions.then;

/** Verifies the relay's string boundary against workflow and event contracts.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@DisplayName("Outbox relay contracts")
class OutboxRelayContractTest {

    @Test
    @DisplayName("workflow type should match the registered workflow interface")
    void workflowTypeShouldMatchRegisteredName() {
        //given
        final String registeredName = POJOWorkflowInterfaceMetadata.newInstance(PublicationWorkflow.class)
            .getWorkflowType()
            .orElseThrow();

        //when
        final String relayName = OutboxRelay.PUBLICATION_WORKFLOW_TYPE;

        //then
        then(PublicationWorkflow.class.isAnnotationPresent(WorkflowInterface.class)).isTrue();
        then(relayName).isEqualTo(registeredName);
        then(Arrays.stream(PublicationWorkflow.class.getMethods())
            .filter(method -> method.isAnnotationPresent(WorkflowMethod.class))).hasSize(1);
    }

    @Test
    @DisplayName("relay payload fields should match integration event records")
    void payloadFieldsShouldMatchIntegrationEventRecords() {
        //given
        final Set<String> startedFields = Arrays.stream(PublicationStartedIntegrationEvent.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());
        final Set<String> publishedFields = Arrays.stream(ArticlePublishedIntegrationEvent.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());

        //when
        final Set<String> relayStartedFields = OutboxRelay.PUBLICATION_STARTED_PAYLOAD_FIELDS;
        final Set<String> relayPublishedFields = OutboxRelay.ARTICLE_PUBLISHED_PAYLOAD_FIELDS;

        //then
        then(relayStartedFields).isEqualTo(startedFields);
        then(relayPublishedFields).isEqualTo(publishedFields);
    }
}
