package com.manning.writeflow.repositories;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.BDDAssertions.then;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.manning.writeflow.shared.event.ArticlePublished;
import com.manning.writeflow.shared.event.DomainEvent;
import com.manning.writeflow.shared.error.Error;

import io.vavr.control.Either;

/**
 * Unit tests for {@link SpringDomainEventPublisher}.
 *
 * @author Enrique Medina Montenegro
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringDomainEventPublisher Tests")
class SpringDomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SpringDomainEventPublisher publisher;

    @Test
    @DisplayName("When publishing domain events, then each event should be republished via Spring")
    void shouldPublishEachEvent() {
        //given
        final ArticlePublished event1 = new ArticlePublished(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            Instant.now());
        final ArticlePublished event2 = new ArticlePublished(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            Instant.now());
        final List<DomainEvent> events = List.of(event1, event2);

        //when
        final Either<Error, Void> result = this.publisher.publish(events);

        //then
        then(result.isRight()).isTrue();
        verify(this.applicationEventPublisher).publishEvent(event1);
        verify(this.applicationEventPublisher).publishEvent(event2);
    }

    @Test
    @DisplayName("When publishing an empty list, then no events should be republished")
    void shouldPublishNothingForEmptyList() {
        //given

        //when
        final Either<Error, Void> result = this.publisher.publish(List.of());

        //then
        then(result.isRight()).isTrue();
        verifyNoInteractions(this.applicationEventPublisher);
    }

}
