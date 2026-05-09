package com.cobre.notification.domain.model;

import java.time.Instant;

public record NotificationEventFilter(
        String clientId,
        Instant from,
        Instant to,
        DeliveryStatus status) {

    public NotificationEventFilter {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be strictly before 'to'");
        }
    }
}
