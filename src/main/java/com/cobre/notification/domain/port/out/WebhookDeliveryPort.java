package com.cobre.notification.domain.port.out;

import com.cobre.notification.domain.model.DeliveryResult;
import com.cobre.notification.domain.model.NotificationEvent;

public interface WebhookDeliveryPort {

    DeliveryResult deliver(NotificationEvent event, String webhookUrl);
}
