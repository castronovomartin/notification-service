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

    private final KafkaTemplate<String, NotificationEventKafkaDto> kafkaTemplate;

    public KafkaNotificationPublisher(KafkaTemplate<String, NotificationEventKafkaDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishForDelivery(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_PENDING, event.getClientId(), NotificationEventKafkaDto.from(event));
    }

    @Override
    public void publishForRetry(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_RETRY, event.getClientId(), NotificationEventKafkaDto.from(event));
    }

    @Override
    public void publishToDlq(NotificationEvent event) {
        kafkaTemplate.send(TOPIC_DLQ, event.getClientId(), NotificationEventKafkaDto.from(event));
    }
}
