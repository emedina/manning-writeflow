package com.manning.writeflow.outboxrelay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import io.temporal.client.WorkflowClient;

import tools.jackson.databind.ObjectMapper;

/** Explicit assembly hook for the relay's scheduled poll.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@Configuration(proxyBeanMethods = false)
public class OutboxRelayScheduling {

    @Bean
    public OutboxRelay outboxRelay(final JdbcTemplate jdbcTemplate, final WorkflowClient workflowClient,
            final ObjectMapper objectMapper) {
        return new OutboxRelay(jdbcTemplate, workflowClient, objectMapper);
    }

    @Bean
    public OutboxRelaySchedule outboxRelaySchedule(final OutboxRelay outboxRelay) {
        return new OutboxRelaySchedule(outboxRelay);
    }

    public static final class OutboxRelaySchedule {

        private final OutboxRelay relay;
        private OutboxRelaySchedule(final OutboxRelay relay) {
            this.relay = relay;
        }

        @Scheduled(fixedDelayString = "${writeflow.outbox-relay.interval:1s}")
        public void poll() {
            this.relay.poll();
        }
    }

}
