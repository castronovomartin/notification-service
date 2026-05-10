package com.cobre.notification.adapter.out.messaging;

import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class KafkaNotificationPublisherTest {

    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private KafkaNotificationPublisher publisher;

    private static final String CLIENT_ID = "CLIENT001";
    private static final String EVENT_ID  = "EVT001";
    private static final Instant NOW      = Instant.parse("2024-03-15T09:30:22Z");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new KafkaNotificationPublisher(kafkaTemplate);
    }

    private NotificationEvent event(DeliveryStatus status) {
        return new NotificationEvent(
                EVENT_ID, "credit_card_payment", "Payment received",
                NOW, status, CLIENT_ID, 0, null, NOW, NOW);
    }

    @Test
    @DisplayName("publishForDelivery sends to notifications.pending with clientId as partition key")
    void publishForDelivery_sendsToCorrectTopicWithClientIdKey() {
        NotificationEvent event = event(DeliveryStatus.PENDING);

        publisher.publishForDelivery(event);

        verify(kafkaTemplate).send(
                KafkaNotificationPublisher.TOPIC_PENDING, CLIENT_ID, event);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("publishForRetry sends to notifications.retry with clientId as partition key")
    void publishForRetry_sendsToCorrectTopicWithClientIdKey() {
        NotificationEvent event = event(DeliveryStatus.RETRYING);

        publisher.publishForRetry(event);

        verify(kafkaTemplate).send(
                KafkaNotificationPublisher.TOPIC_RETRY, CLIENT_ID, event);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("publishToDlq sends to notifications.dlq with clientId as partition key")
    void publishToDlq_sendsToCorrectTopicWithClientIdKey() {
        NotificationEvent event = event(DeliveryStatus.FAILED);

        publisher.publishToDlq(event);

        verify(kafkaTemplate).send(
                KafkaNotificationPublisher.TOPIC_DLQ, CLIENT_ID, event);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("partition key is always clientId regardless of event content")
    void partitionKey_isAlwaysClientId() {
        NotificationEvent event = event(DeliveryStatus.PENDING);
        String expectedKey = event.getClientId();

        publisher.publishForDelivery(event);

        verify(kafkaTemplate).send(
                KafkaNotificationPublisher.TOPIC_PENDING, expectedKey, event);
    }
}
