package com.cobre.notification.adapter.in.rest.mapper;

import com.cobre.notification.adapter.in.rest.dto.NotificationEventResponse;
import com.cobre.notification.domain.model.NotificationEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventRestMapper {

    public NotificationEventResponse toResponse(NotificationEvent event) {
        return new NotificationEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getContent(),
                event.getDeliveryDate(),
                event.getStatus(),
                event.getClientId(),
                event.getRetryCount(),
                event.getLastAttemptAt(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
