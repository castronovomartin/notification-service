package com.cobre.notification.adapter.out.messaging;

import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.port.out.NotificationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationPublisher implements NotificationEventPublisher {

    static final String TOPIC_PENDING = "notifications.pending";
    static final String TOPIC_RETRY   = "notifications.retry";
    static final String TOPIC_DLQ     = "notifications.dlq";

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public KafkaNotificationPublisher(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishForDelivery(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_PENDING, event.getClientId(), event);
    }

    @Override
    public void publishForRetry(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_RETRY, event.getClientId(), event);
    }

    @Override
    public void publishToDlq(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_DLQ, event.getClientId(), event);
    }
}
