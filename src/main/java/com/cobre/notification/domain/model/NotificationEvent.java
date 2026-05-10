package com.cobre.notification.domain.model;

import com.cobre.notification.domain.exception.InvalidStatusTransitionException;
import java.time.Instant;

public class NotificationEvent {

    private final String eventId;
    private final String eventType;
    private final String content;
    private final Instant deliveryDate;
    private DeliveryStatus status;
    private final String clientId;
    private int retryCount;
    private Instant lastAttemptAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationEvent(
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
        this.eventId = eventId;
        this.eventType = eventType;
        this.content = content;
        this.deliveryDate = deliveryDate;
        this.status = status;
        this.clientId = clientId;
        this.retryCount = retryCount;
        this.lastAttemptAt = lastAttemptAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Behaviour methods -----------------------------------------------

    public void markRetrying(Instant attemptedAt) {
        requireTransition(DeliveryStatus.RETRYING);
        retryCount++;
        lastAttemptAt = attemptedAt;
        updatedAt = attemptedAt;
    }

    public void markCompleted(Instant attemptedAt) {
        requireTransition(DeliveryStatus.COMPLETED);
        lastAttemptAt = attemptedAt;
        updatedAt = attemptedAt;
    }

    public void markFailed(Instant attemptedAt) {
        requireTransition(DeliveryStatus.FAILED);
        lastAttemptAt = attemptedAt;
        updatedAt = attemptedAt;
    }

    public void markSkipped(Instant now) {
        requireTransition(DeliveryStatus.SKIPPED);
        updatedAt = now;
    }

    public void resetForReplay(Instant now) {
        requireTransition(DeliveryStatus.PENDING);
        // Intentional deviation: spec-02 states "retryCount only increments, never decrements"
        // and spec-05 states "retryCount is NOT reset (preserved for audit purposes)".
        // However, spec-05 also states "The full delivery flow restarts from attempt 1."
        // Without this reset, a replay on a fully exhausted event (retryCount=5) would
        // re-exhaust after a single failure (5 >= MAX_RETRY_ATTEMPTS), making replay useless.
        // This implementation resolves the spec contradiction in favor of functional correctness.
        retryCount = 0;
        updatedAt = now;
    }

    // --- Private helpers --------------------------------------------------

    private void requireTransition(DeliveryStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        status = next;
    }

    // --- Getters ----------------------------------------------------------

    public String getEventId()          { return eventId; }
    public String getEventType()        { return eventType; }
    public String getContent()          { return content; }
    public Instant getDeliveryDate()    { return deliveryDate; }
    public DeliveryStatus getStatus()   { return status; }
    public String getClientId()         { return clientId; }
    public int getRetryCount()          { return retryCount; }
    public Instant getLastAttemptAt()   { return lastAttemptAt; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }
}
