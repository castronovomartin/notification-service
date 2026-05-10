package com.cobre.notification.adapter.in.messaging;

import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationEventUseCase useCase;

    public NotificationEventConsumer(NotificationEventUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(
            topics = {"notifications.pending", "notifications.retry"},
            groupId = "${spring.kafka.consumer.group-id:notification-dispatcher}")
    public void consume(ConsumerRecord<String, NotificationEvent> record) {
        try {
            MDC.put("correlationId", UUID.randomUUID().toString());
            MDC.put("clientId", record.key());
            MDC.put("eventId", record.value().getEventId());
            MDC.put("topic", record.topic());
            MDC.put("partition", String.valueOf(record.partition()));

            log.info("Processing notification event");
            useCase.processEvent(record.value());
        } finally {
            MDC.clear();
        }
    }
}
