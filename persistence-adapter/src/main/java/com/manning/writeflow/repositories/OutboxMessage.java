package com.manning.writeflow.repositories;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence representation of a transactional outbox row.
 *
 * <p>Each row captures a single integration event that must be reliably relayed to external
 * consumers. The row is written within the same transaction that mutates the aggregate, so
 * that event publication is atomic with the business state change (the transactional-outbox
 * pattern). A separate relay process (out of scope for this chapter) reads unpublished rows
 * and dispatches them, stamping {@code publishedAt} once delivered.</p>
 *
 * @author Enrique Medina Montenegro
 */
@Entity
@Table(name = "integration_event_outbox")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class OutboxMessage {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Convenience constructor used by the publisher to build a fully-populated outbox row.
     *
     * @param eventId     the unique identifier of the integration event
     * @param eventType   the logical type of the integration event
     * @param payload     the serialized JSON payload of the integration event
     * @param createdAt   the instant at which the row was created
     * @param publishedAt the instant at which the event was relayed, or {@code null} if pending
     */
    public OutboxMessage(final String eventId, final String eventType, final String payload,
            final Instant createdAt, final Instant publishedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

}
