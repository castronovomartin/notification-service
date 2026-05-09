package com.cobre.notification.adapter.in.rest.dto;

import com.cobre.notification.domain.model.DeliveryStatus;
import java.time.Instant;

public record NotificationEventResponse(
        String eventId,
        String eventType,
        String content,
        Instant deliveryDate,
        DeliveryStatus status,
        String clientId,
        int retryCount,
        Instant lastAttemptAt,
        Instant createdAt,
        Instant updatedAt) {
}
