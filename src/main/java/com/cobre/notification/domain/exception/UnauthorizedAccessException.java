package com.cobre.notification.domain.exception;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String eventId, String clientId) {
        super("Client '" + clientId + "' is not authorized to access event '" + eventId + "'");
    }
}
