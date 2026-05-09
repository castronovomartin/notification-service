package com.cobre.notification.domain.exception;

import com.cobre.notification.domain.model.DeliveryStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final DeliveryStatus from;
    private final DeliveryStatus to;

    public InvalidStatusTransitionException(DeliveryStatus from, DeliveryStatus to) {
        super(String.format("Invalid status transition: %s -> %s", from, to));
        this.from = from;
        this.to = to;
    }

    public DeliveryStatus getFrom() { return from; }
    public DeliveryStatus getTo()   { return to; }
}
