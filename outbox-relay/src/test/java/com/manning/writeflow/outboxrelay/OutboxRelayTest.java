package com.manning.writeflow.outboxrelay;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowStub;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** JDBC-backed behavior tests for the outbox relay.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@DisplayName("Outbox relay")
class OutboxRelayTest {

    private JdbcTemplate jdbcTemplate;
    private WorkflowClient workflowClient;
    private WorkflowStub workflowStub;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() throws SQLException {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:relay;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS integration_event_outbox");
        this.jdbcTemplate.execute("CREATE TABLE integration_event_outbox (event_id VARCHAR(36) PRIMARY KEY, "
            + "event_type VARCHAR(100) NOT NULL, payload CLOB NOT NULL, created_at TIMESTAMP NOT NULL, published_at TIMESTAMP)");
        this.workflowClient = mock(WorkflowClient.class);
        this.workflowStub = mock(WorkflowStub.class);
        given(this.workflowClient.newUntypedWorkflowStub(anyString(), any())).willReturn(this.workflowStub);
        this.relay = new OutboxRelay(this.jdbcTemplate, this.workflowClient, new ObjectMapper());
    }

    @Test
    @DisplayName("poll should start and mark a PublicationStarted row")
    void pollShouldStartAndMarkPublicationStarted() {
        //given
        this.insert("event-1", "PublicationStarted", "{\"eventId\":\"event-1\",\"articleId\":\"article-1\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 1);

        //when
        this.relay.poll();

        //then
        then(this.publishedAt("event-1")).isNotNull();
        verify(this.workflowStub).start("article-1");
    }

    @Test
    @DisplayName("poll should start publication workflows in creation order")
    void pollShouldStartWorkflowsInCreationOrder() {
        //given
        this.insert("later-event", "PublicationStarted", "{\"eventId\":\"later-event\",\"articleId\":\"later-article\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 2);
        this.insert("earlier-event", "PublicationStarted", "{\"eventId\":\"earlier-event\",\"articleId\":\"earlier-article\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 1);

        //when
        this.relay.poll();

        //then
        final org.mockito.InOrder order = org.mockito.Mockito.inOrder(this.workflowStub);
        order.verify(this.workflowStub).start("earlier-article");
        order.verify(this.workflowStub).start("later-article");
    }

    @Test
    @DisplayName("poll should mark an already-started workflow")
    void pollShouldMarkAlreadyStartedWorkflow() {
        //given
        this.insert("event-2", "PublicationStarted", "{\"eventId\":\"event-2\",\"articleId\":\"article-2\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 1);
        willThrow(mock(WorkflowExecutionAlreadyStarted.class))
            .given(this.workflowStub).start("article-2");

        //when
        this.relay.poll();

        //then
        then(this.publishedAt("event-2")).isNotNull();
    }

    @Test
    @DisplayName("poll should leave a failed row pending and continue to the next row")
    void pollShouldIsolateStartFailureAndContinue() {
        //given
        this.insert("event-3", "PublicationStarted", "{\"eventId\":\"event-3\",\"articleId\":\"article-3\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 1);
        this.insert("event-4", "ArticlePublished", "{\"eventId\":\"event-4\",\"articleId\":\"article-4\",\"publishedAt\":\"2026-01-01T00:00:00Z\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 2);
        willThrow(new IllegalStateException("Temporal unavailable")).given(this.workflowStub).start("article-3");

        //when
        this.relay.poll();

        //then
        then(this.publishedAt("event-3")).isNull();
        then(this.publishedAt("event-4")).isNotNull();
    }

    @Test
    @DisplayName("poll should retry marking a started row on the next poll")
    void pollShouldRetryMarkAfterUpdateFailure() {
        //given
        this.insert("event-5", "PublicationStarted", "{\"eventId\":\"event-5\",\"articleId\":\"article-5\",\"createdAt\":\"2026-01-01T00:00:00Z\"}", 1);
        this.jdbcTemplate.execute("CREATE TRIGGER fail_first_mark BEFORE UPDATE ON integration_event_outbox "
            + "FOR EACH ROW CALL 'com.manning.writeflow.outboxrelay.FailFirstMarkTrigger'");

        //when
        this.relay.poll();
        this.relay.poll();

        //then
        then(this.publishedAt("event-5")).isNotNull();
        verify(this.workflowStub, org.mockito.Mockito.times(2)).start("article-5");
    }

    @Test
    @DisplayName("poll should leave unknown and malformed events pending")
    void pollShouldSkipUnknownAndMalformedEvents() {
        //given
        this.insert("event-6", "Mystery", "{}", 1);
        this.insert("event-7", "PublicationStarted", "{broken", 2);
        this.insert("event-8", "PublicationStarted", "{}", 3);

        //when
        this.relay.poll();

        //then
        then(this.publishedAt("event-6")).isNull();
        then(this.publishedAt("event-7")).isNull();
        then(this.publishedAt("event-8")).isNull();
        verifyNoInteractions(this.workflowClient);
    }

    @Test
    @DisplayName("poll should leave a row pending when its payload event id differs")
    void pollShouldLeaveRowPendingWhenPayloadEventIdDiffers() {
        //given
        this.insert("event-9", "PublicationStarted",
            "{\"eventId\":\"different-event\",\"articleId\":\"article-9\",\"createdAt\":\"2026-01-01T00:00:00Z\"}",
            1);

        //when
        this.relay.poll();

        //then
        then(this.publishedAt("event-9")).isNull();
        verifyNoInteractions(this.workflowClient);
    }

    private void insert(final String eventId, final String eventType, final String payload, final int second) {
        this.jdbcTemplate.update("INSERT INTO integration_event_outbox VALUES (?, ?, ?, ?, NULL)", eventId, eventType,
            payload, Timestamp.from(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(second)));
    }

    private Instant publishedAt(final String eventId) {
        final Timestamp publishedAt = this.jdbcTemplate.queryForObject(
            "SELECT published_at FROM integration_event_outbox WHERE event_id = ?", Timestamp.class, eventId);
        return publishedAt == null ? null : publishedAt.toInstant();
    }
}
