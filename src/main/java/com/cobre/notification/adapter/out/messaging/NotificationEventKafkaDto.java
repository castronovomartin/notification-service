package com.cobre.notification.adapter.out.messaging;

import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;

import java.time.Instant;

public class NotificationEventKafkaDto {

    private String eventId;
    private String eventType;
    private String content;
    private Instant deliveryDate;
    private DeliveryStatus status;
    private String clientId;
    private int retryCount;
    private Instant lastAttemptAt;
    private Instant createdAt;
    private Instant updatedAt;

    public NotificationEventKafkaDto() {}

    public static NotificationEventKafkaDto from(NotificationEvent event) {
        NotificationEventKafkaDto dto = new NotificationEventKafkaDto();
        dto.eventId       = event.getEventId();
        dto.eventType     = event.getEventType();
        dto.content       = event.getContent();
        dto.deliveryDate  = event.getDeliveryDate();
        dto.status        = event.getStatus();
        dto.clientId      = event.getClientId();
        dto.retryCount    = event.getRetryCount();
        dto.lastAttemptAt = event.getLastAttemptAt();
        dto.createdAt     = event.getCreatedAt();
        dto.updatedAt     = event.getUpdatedAt();
        return dto;
    }

    public NotificationEvent toDomain() {
        return new NotificationEvent(
                eventId, eventType, content, deliveryDate,
                status, clientId, retryCount,
                lastAttemptAt, createdAt, updatedAt);
    }

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

    public void setEventId(String eventId)             { this.eventId = eventId; }
    public void setEventType(String eventType)         { this.eventType = eventType; }
    public void setContent(String content)             { this.content = content; }
    public void setDeliveryDate(Instant deliveryDate)  { this.deliveryDate = deliveryDate; }
    public void setStatus(DeliveryStatus status)       { this.status = status; }
    public void setClientId(String clientId)           { this.clientId = clientId; }
    public void setRetryCount(int retryCount)          { this.retryCount = retryCount; }
    public void setLastAttemptAt(Instant lastAttemptAt){ this.lastAttemptAt = lastAttemptAt; }
    public void setCreatedAt(Instant createdAt)        { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt)        { this.updatedAt = updatedAt; }
}
