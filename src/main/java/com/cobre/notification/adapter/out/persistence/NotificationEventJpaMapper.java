package com.cobre.notification.adapter.out.persistence;

import com.cobre.notification.domain.model.NotificationEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventJpaMapper {

    public NotificationEvent toDomain(NotificationEventJpaEntity entity) {
        return new NotificationEvent(
                entity.getEventId(),
                entity.getEventType(),
                entity.getContent(),
                entity.getDeliveryDate(),
                entity.getStatus(),
                entity.getClientId(),
                entity.getRetryCount(),
                entity.getLastAttemptAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public NotificationEventJpaEntity toEntity(NotificationEvent domain) {
        return new NotificationEventJpaEntity(
                domain.getEventId(),
                domain.getEventType(),
                domain.getContent(),
                domain.getDeliveryDate(),
                domain.getStatus(),
                domain.getClientId(),
                domain.getRetryCount(),
                domain.getLastAttemptAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
