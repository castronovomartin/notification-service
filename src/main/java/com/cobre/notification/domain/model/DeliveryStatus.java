package com.cobre.notification.domain.model;

public enum DeliveryStatus {

    PENDING,
    RETRYING,
    COMPLETED,
    FAILED,
    SKIPPED;

    public boolean canTransitionTo(DeliveryStatus next) {
        return switch (this) {
            case PENDING   -> next == COMPLETED || next == RETRYING || next == SKIPPED;
            case RETRYING  -> next == COMPLETED || next == RETRYING || next == FAILED;
            case FAILED    -> next == PENDING;
            case COMPLETED, SKIPPED -> false;
        };
    }
}
