package com.manning.writeflow.shared.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link PublicationStarted}.
 *
 * @author Enrique Medina Montenegro
 */
class PublicationStartedTest {

    @Test
    @DisplayName("When event payloads match, then PublicationStarted events should be equal")
    void publicationStartedShouldCompareEqualWhenPayloadMatches() {
        //given
        final PublicationStarted expected = new PublicationStarted("article-1", "author-1");

        //when
        final PublicationStarted event = new PublicationStarted("article-1", "author-1");

        //then
        then(event).isEqualTo(expected);
    }

}
