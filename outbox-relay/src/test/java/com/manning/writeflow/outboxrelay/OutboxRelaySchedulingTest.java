package com.manning.writeflow.outboxrelay;

import io.temporal.client.WorkflowClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Tests the explicit Spring wiring without starting a container.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@DisplayName("Outbox relay scheduling")
class OutboxRelaySchedulingTest {

    @Test
    @DisplayName("configuration should expose relay and schedule beans")
    void configurationShouldExposeRelayAndSchedule() {
        //given
        final OutboxRelayScheduling configuration = new OutboxRelayScheduling();
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        final WorkflowClient workflowClient = mock(WorkflowClient.class);
        final ObjectMapper objectMapper = new ObjectMapper();
        given(jdbcTemplate.query(anyString(), any(RowMapper.class))).willReturn(java.util.List.of());

        //when
        final OutboxRelay relay = configuration.outboxRelay(jdbcTemplate, workflowClient, objectMapper);
        final OutboxRelayScheduling.OutboxRelaySchedule schedule = configuration.outboxRelaySchedule(relay);
        schedule.poll();

        //then
        then(relay).isNotNull();
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }
}
