package com.manning.writeflow.shared.dto;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NotificationTextDTO}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("NotificationTextDTO Tests")
class NotificationTextDTOTest {

    @Test
    @DisplayName("When creating with a value, then should store the value")
    void shouldStoreValue_whenValueProvided() {
        // when
        final NotificationTextDTO result = new NotificationTextDTO("hello");

        // then
        then(result.value()).isEqualTo("hello");
        then(result.toString()).contains("NotificationTextDTO").contains("hello");
    }

    @Test
    @DisplayName("When creating with null, then should throw NullPointerException")
    void shouldThrow_whenNullProvided() {
        // then
        thenThrownBy(() -> new NotificationTextDTO(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("When comparing equal DTOs, then should be equal")
    void shouldBeEqual_whenSameValueProvided() {
        // given
        final NotificationTextDTO a = new NotificationTextDTO("hello");
        final NotificationTextDTO b = new NotificationTextDTO("hello");

        // then
        then(a).isEqualTo(b);
        then(a.hashCode()).isEqualTo(b.hashCode());
    }
}
