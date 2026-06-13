package com.manning.writeflow.shared.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Plain data-transfer object carrying the textual body of a notification.
 *
 * @author Enrique Medina Montenegro
 */
public record NotificationTextDTO(String value) implements Serializable {

    /**
     * Canonical constructor guarding against a {@code null} value.
     *
     * @param value the notification text
     */
    public NotificationTextDTO {
        Objects.requireNonNull(value, "value must not be null");
    }

}
