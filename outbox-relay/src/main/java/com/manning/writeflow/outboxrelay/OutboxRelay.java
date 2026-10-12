package com.manning.writeflow.outboxrelay;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Polls the durable outbox and hands events to their external destinations.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
public final class OutboxRelay {

    public static final String PUBLICATION_WORKFLOW_TYPE = "PublicationWorkflow";
    public static final Set<String> PUBLICATION_STARTED_PAYLOAD_FIELDS = Set.of("eventId", "articleId", "createdAt");
    public static final Set<String> ARTICLE_PUBLISHED_PAYLOAD_FIELDS =
        Set.of("eventId", "articleId", "publishedAt", "createdAt");
    private static final String PUBLICATION_TASK_QUEUE = "publication";
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);
    private static final SearchAttributeKey<String> ARTICLE_ID = SearchAttributeKey.forKeyword("ArticleId");

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper;

    public OutboxRelay(final JdbcTemplate jdbcTemplate, final WorkflowClient workflowClient,
            final ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowClient = workflowClient;
        this.objectMapper = objectMapper;
    }

    /** Processes one ordered snapshot, isolating every row's failure from the rest of the batch. */
    public void poll() {
        final List<OutboxRow> rows = this.jdbcTemplate.query(
            "SELECT event_id, event_type, payload FROM integration_event_outbox "
                + "WHERE published_at IS NULL ORDER BY created_at, event_id",
            (resultSet, rowNumber) -> new OutboxRow(resultSet.getString("event_id"),
                resultSet.getString("event_type"), resultSet.getString("payload")));
        for (final OutboxRow row : rows) {
            try {
                if (this.dispatch(row)) {
                    this.jdbcTemplate.update("UPDATE integration_event_outbox SET published_at = ? "
                        + "WHERE event_id = ? AND published_at IS NULL", Timestamp.from(Instant.now()), row.eventId());
                }
            } catch (final RuntimeException failure) {
                LOGGER.warn("Outbox event {} dispatch failed; it will be retried", row.eventId(), failure);
            }
        }
    }

    private boolean dispatch(final OutboxRow row) {
        if ("ArticlePublished".equals(row.eventType())) {
            final JsonNode payload = this.validPayload(row, ARTICLE_PUBLISHED_PAYLOAD_FIELDS);
            if (payload == null) {
                return false;
            }
            LOGGER.info("would publish to broker: ArticlePublished eventId={} articleId={}",
                payload.get("eventId").asString(), payload.get("articleId").asString());
            return true;
        }
        if ("PublicationStarted".equals(row.eventType())) {
            final JsonNode payload = this.validPayload(row, PUBLICATION_STARTED_PAYLOAD_FIELDS);
            if (payload == null) {
                return false;
            }
            final String eventId = payload.get("eventId").asString();
            final String articleId = payload.get("articleId").asString();
            final WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("publication-" + articleId + "-" + eventId)
                .setTaskQueue(PUBLICATION_TASK_QUEUE)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .setTypedSearchAttributes(SearchAttributes.newBuilder().set(ARTICLE_ID, articleId).build())
                .build();
            try {
                final WorkflowStub workflowStub = this.workflowClient.newUntypedWorkflowStub(
                    PUBLICATION_WORKFLOW_TYPE, options);
                workflowStub.start(articleId);
            } catch (final WorkflowExecutionAlreadyStarted alreadyStarted) {
                LOGGER.info("Publication workflow already started for event {}", eventId);
            }
            return true;
        }
        LOGGER.warn("Skipping unknown outbox event type {} for event {}", row.eventType(), row.eventId());
        return false;
    }

    private JsonNode validPayload(final OutboxRow row, final Set<String> fields) {
        try {
            final JsonNode payload = this.objectMapper.readTree(row.payload());
            for (final String field : fields) {
                if (payload == null || !payload.has(field) || payload.get(field).isNull()
                        || !payload.get(field).isString() || payload.get(field).asString().isBlank()) {
                    LOGGER.warn("Skipping malformed {} outbox event {}: missing {}", row.eventType(), row.eventId(), field);
                    return null;
                }
            }
            if (!row.eventId().equals(payload.get("eventId").asString())) {
                LOGGER.warn("Skipping malformed {} outbox event {}: payload eventId does not match row",
                    row.eventType(), row.eventId());
                return null;
            }
            return payload;
        } catch (final RuntimeException malformed) {
            LOGGER.warn("Skipping malformed {} outbox event {}", row.eventType(), row.eventId(), malformed);
            return null;
        }
    }

    private record OutboxRow(String eventId, String eventType, String payload) {
    }
}
