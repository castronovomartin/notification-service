package com.cobre.notification.domain.port.out;

import com.cobre.notification.domain.model.NotificationEvent;

public interface NotificationEventPublisher {

    void publishForDelivery(NotificationEvent event);

    void publishForRetry(NotificationEvent event);

    void publishToDlq(NotificationEvent event);
}
