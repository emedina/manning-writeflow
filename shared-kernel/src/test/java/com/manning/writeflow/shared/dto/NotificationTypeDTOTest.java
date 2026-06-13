package com.manning.writeflow.shared.dto;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NotificationTypeDTO}.
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("NotificationTypeDTO Tests")
class NotificationTypeDTOTest {

    @Test
    @DisplayName("When resolving ARTICLE_PUBLISHED by name, then should return the enum constant")
    void shouldResolveConstant_whenValueOfCalled() {
        // when
        final NotificationTypeDTO result = NotificationTypeDTO.valueOf("ARTICLE_PUBLISHED");

        // then
        then(result).isEqualTo(NotificationTypeDTO.ARTICLE_PUBLISHED);
        then(NotificationTypeDTO.values()).containsExactly(NotificationTypeDTO.ARTICLE_PUBLISHED);
    }
}
