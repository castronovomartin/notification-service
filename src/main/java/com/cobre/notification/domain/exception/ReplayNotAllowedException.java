package com.cobre.notification.domain.exception;

import com.cobre.notification.domain.model.DeliveryStatus;

public class ReplayNotAllowedException extends RuntimeException {

    public ReplayNotAllowedException(String eventId, DeliveryStatus currentStatus) {
        super("Replay not allowed for event '" + eventId + "' with status " + currentStatus
              + ". Only FAILED events can be replayed.");
    }
}
