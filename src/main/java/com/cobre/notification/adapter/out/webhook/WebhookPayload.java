package com.cobre.notification.adapter.out.webhook;

import java.time.Instant;

record WebhookPayload(
        String eventId,
        String eventType,
        String content,
        Instant deliveryDate,
        String clientId,
        int retryCount,
        Instant deliveredAt) {
}
