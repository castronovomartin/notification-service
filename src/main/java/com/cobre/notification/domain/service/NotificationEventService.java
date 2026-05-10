package com.cobre.notification.domain.service;

import com.cobre.notification.domain.exception.EventNotFoundException;
import com.cobre.notification.domain.exception.NonRetryableDeliveryException;
import com.cobre.notification.domain.exception.ReplayNotAllowedException;
import com.cobre.notification.domain.exception.UnauthorizedAccessException;
import com.cobre.notification.domain.model.DeliveryResult;
import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import com.cobre.notification.domain.model.Subscription;
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import com.cobre.notification.domain.port.out.NotificationEventPublisher;
import com.cobre.notification.domain.port.out.NotificationEventRepository;
import com.cobre.notification.domain.port.out.SubscriptionPort;
import com.cobre.notification.domain.port.out.WebhookDeliveryPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class NotificationEventService implements NotificationEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventService.class);

    static final int MAX_RETRY_ATTEMPTS = 5;

    private final NotificationEventRepository repository;
    private final WebhookDeliveryPort webhookDeliveryPort;
    private final SubscriptionPort subscriptionPort;
    private final NotificationEventPublisher publisher;
    private final MeterRegistry meterRegistry;

    public NotificationEventService(
            NotificationEventRepository repository,
            WebhookDeliveryPort webhookDeliveryPort,
            SubscriptionPort subscriptionPort,
            NotificationEventPublisher publisher,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.webhookDeliveryPort = webhookDeliveryPort;
        this.subscriptionPort = subscriptionPort;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public NotificationEvent findById(String eventId, String clientId) {
        NotificationEvent event = repository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getClientId().equals(clientId)) {
            throw new UnauthorizedAccessException(eventId, clientId);
        }
        return event;
    }

    @Override
    public PagedResult<NotificationEvent> findAll(NotificationEventFilter filter, PageRequest pageRequest) {
        return repository.findAll(filter, pageRequest);
    }

    @Override
    public void replay(String eventId, String clientId) {
        NotificationEvent event = findById(eventId, clientId);
        if (event.getStatus() != DeliveryStatus.FAILED) {
            throw new ReplayNotAllowedException(eventId, event.getStatus());
        }
        event.resetForReplay(Instant.now());
        repository.save(event);
        publisher.publishForDelivery(event);
        recordReplayed(event);
    }

    @Override
    public void processEvent(NotificationEvent event) {
        Optional<Subscription> subscription =
                subscriptionPort.findActiveByClientId(event.getClientId());

        if (subscription.isEmpty() || !subscription.get().accepts(event.getEventType())) {
            event.markSkipped(Instant.now());
            repository.save(event);
            recordSkipped(event);
            return;
        }

        String webhookUrl = subscription.get().getWebhookUrl();
        try {
            DeliveryResult result = webhookDeliveryPort.deliver(event, webhookUrl);
            Instant attemptedAt = result.attemptedAt();

            if (result.success()) {
                event.markCompleted(attemptedAt);
                repository.save(event);
                log.info("Notification delivered successfully");
                recordDelivered(event, result);
            } else {
                event.markRetrying(attemptedAt);
                if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                    event.markFailed(attemptedAt);
                    repository.save(event);
                    publisher.publishToDlq(event);
                    log.error("All retry attempts exhausted, publishing to DLQ");
                    recordFailed(event, "exhausted", result);
                } else {
                    repository.save(event);
                    publisher.publishForRetry(event);
                    recordRetried(event);
                }
            }
        } catch (NonRetryableDeliveryException e) {
            Instant now = Instant.now();
            event.markRetrying(now);
            event.markFailed(now);
            repository.save(event);
            publisher.publishToDlq(event);
            log.error("Non-retryable delivery failure, publishing to DLQ");
            recordFailedNonRetryable(event);
        }
    }

    // -----------------------------------------------------------------------
    // Metrics helpers
    // -----------------------------------------------------------------------

    private void recordDelivered(NotificationEvent event, DeliveryResult result) {
        meterRegistry.counter("notifications.delivered",
                "client_id", event.getClientId(),
                "event_type", event.getEventType()
        ).increment();

        meterRegistry.timer("notifications.delivery.duration",
                "client_id", event.getClientId(),
                "event_type", event.getEventType(),
                "outcome", "success"
        ).record(Duration.between(event.getCreatedAt(), result.attemptedAt()));
    }

    private void recordFailed(NotificationEvent event, String reason, DeliveryResult result) {
        meterRegistry.counter("notifications.failed",
                "client_id", event.getClientId(),
                "event_type", event.getEventType(),
                "reason", reason
        ).increment();

        meterRegistry.timer("notifications.delivery.duration",
                "client_id", event.getClientId(),
                "event_type", event.getEventType(),
                "outcome", "failure"
        ).record(Duration.between(event.getCreatedAt(), result.attemptedAt()));
    }

    private void recordFailedNonRetryable(NotificationEvent event) {
        meterRegistry.counter("notifications.failed",
                "client_id", event.getClientId(),
                "event_type", event.getEventType(),
                "reason", "non_retryable"
        ).increment();

        meterRegistry.timer("notifications.delivery.duration",
                "client_id", event.getClientId(),
                "event_type", event.getEventType(),
                "outcome", "failure"
        ).record(Duration.between(event.getCreatedAt(), Instant.now()));
    }

    private void recordRetried(NotificationEvent event) {
        meterRegistry.counter("notifications.retried",
                "client_id", event.getClientId(),
                "event_type", event.getEventType()
        ).increment();
    }

    private void recordSkipped(NotificationEvent event) {
        meterRegistry.counter("notifications.skipped",
                "client_id", event.getClientId(),
                "event_type", event.getEventType()
        ).increment();
    }

    private void recordReplayed(NotificationEvent event) {
        meterRegistry.counter("notifications.replayed",
                "client_id", event.getClientId()
        ).increment();
    }
}
