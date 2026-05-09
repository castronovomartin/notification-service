package com.cobre.notification.domain.exception;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Notification event not found: " + eventId);
    }
}
