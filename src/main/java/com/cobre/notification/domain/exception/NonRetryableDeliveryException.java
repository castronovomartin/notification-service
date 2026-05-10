package com.cobre.notification.domain.exception;

public class NonRetryableDeliveryException extends RuntimeException {

    public NonRetryableDeliveryException(String eventId, int httpStatusCode) {
        super("Non-retryable delivery failure for event '" + eventId
              + "': HTTP " + httpStatusCode + " — permanent client-side error");
    }
}
