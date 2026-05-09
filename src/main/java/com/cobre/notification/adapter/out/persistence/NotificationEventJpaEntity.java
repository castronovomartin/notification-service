package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.domain.model.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "notification_events",
        indexes = @Index(
                name = "idx_notification_events_client_delivery_status",
                columnList = "client_id, delivery_date, delivery_status"))
public class NotificationEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "delivery_date", nullable = false)
    private Instant deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Required by JPA; also used by Jackson for seed-data deserialization
    public NotificationEventJpaEntity() {}

    // Used by the mapper for explicit construction
    NotificationEventJpaEntity(
            String eventId, String eventType, String content,
            Instant deliveryDate, DeliveryStatus status, String clientId,
            int retryCount, Instant lastAttemptAt, Instant createdAt, Instant updatedAt) {
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

    // --- Getters and setters (setters required for Jackson deserialization) ---

    public String getEventId()                  { return eventId; }
    public void   setEventId(String v)          { this.eventId = v; }

    public String getEventType()                { return eventType; }
    public void   setEventType(String v)        { this.eventType = v; }

    public String getContent()                  { return content; }
    public void   setContent(String v)          { this.content = v; }

    public Instant getDeliveryDate()            { return deliveryDate; }
    public void    setDeliveryDate(Instant v)   { this.deliveryDate = v; }

    public DeliveryStatus getStatus()           { return status; }
    public void           setStatus(DeliveryStatus v) { this.status = v; }

    public String getClientId()                 { return clientId; }
    public void   setClientId(String v)         { this.clientId = v; }

    public int  getRetryCount()                 { return retryCount; }
    public void setRetryCount(int v)            { this.retryCount = v; }

    public Instant getLastAttemptAt()           { return lastAttemptAt; }
    public void    setLastAttemptAt(Instant v)  { this.lastAttemptAt = v; }

    public Instant getCreatedAt()               { return createdAt; }
    public void    setCreatedAt(Instant v)      { this.createdAt = v; }

    public Instant getUpdatedAt()               { return updatedAt; }
    public void    setUpdatedAt(Instant v)      { this.updatedAt = v; }
}
