package com.cobre.notification.adapter.out.messaging;

import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class KafkaNotificationPublisherTest {

    private KafkaTemplate<String, NotificationEventKafkaDto> kafkaTemplate;
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
        ArgumentCaptor<NotificationEventKafkaDto> captor = ArgumentCaptor.forClass(NotificationEventKafkaDto.class);

        publisher.publishForDelivery(event);

        verify(kafkaTemplate).send(
                eq(KafkaNotificationPublisher.TOPIC_PENDING), eq(CLIENT_ID), captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getClientId()).isEqualTo(CLIENT_ID);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("publishForRetry sends to notifications.retry with clientId as partition key")
    void publishForRetry_sendsToCorrectTopicWithClientIdKey() {
        NotificationEvent event = event(DeliveryStatus.RETRYING);
        ArgumentCaptor<NotificationEventKafkaDto> captor = ArgumentCaptor.forClass(NotificationEventKafkaDto.class);

        publisher.publishForRetry(event);

        verify(kafkaTemplate).send(
                eq(KafkaNotificationPublisher.TOPIC_RETRY), eq(CLIENT_ID), captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getClientId()).isEqualTo(CLIENT_ID);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("publishToDlq sends to notifications.dlq with clientId as partition key")
    void publishToDlq_sendsToCorrectTopicWithClientIdKey() {
        NotificationEvent event = event(DeliveryStatus.FAILED);
        ArgumentCaptor<NotificationEventKafkaDto> captor = ArgumentCaptor.forClass(NotificationEventKafkaDto.class);

        publisher.publishToDlq(event);

        verify(kafkaTemplate).send(
                eq(KafkaNotificationPublisher.TOPIC_DLQ), eq(CLIENT_ID), captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getClientId()).isEqualTo(CLIENT_ID);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("partition key is always clientId regardless of event content")
    void partitionKey_isAlwaysClientId() {
        NotificationEvent event = event(DeliveryStatus.PENDING);
        ArgumentCaptor<NotificationEventKafkaDto> captor = ArgumentCaptor.forClass(NotificationEventKafkaDto.class);

        publisher.publishForDelivery(event);

        verify(kafkaTemplate).send(
                eq(KafkaNotificationPublisher.TOPIC_PENDING), eq(event.getClientId()), captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(event.getClientId());
    }
}
