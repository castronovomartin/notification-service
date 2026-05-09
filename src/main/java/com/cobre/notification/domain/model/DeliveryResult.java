package com.cobre.notification.domain.model;

import java.time.Duration;
import java.time.Instant;

public record DeliveryResult(
        boolean success,
        int httpStatusCode,
        String errorMessage,
        Duration responseTime,
        Instant attemptedAt) {}
