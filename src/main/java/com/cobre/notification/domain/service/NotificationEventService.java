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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class NotificationEventService implements NotificationEventUseCase {

    static final int MAX_RETRY_ATTEMPTS = 5;

    private final NotificationEventRepository repository;
    private final WebhookDeliveryPort webhookDeliveryPort;
    private final SubscriptionPort subscriptionPort;
    private final NotificationEventPublisher publisher;

    public NotificationEventService(
            NotificationEventRepository repository,
            WebhookDeliveryPort webhookDeliveryPort,
            SubscriptionPort subscriptionPort,
            NotificationEventPublisher publisher) {
        this.repository = repository;
        this.webhookDeliveryPort = webhookDeliveryPort;
        this.subscriptionPort = subscriptionPort;
        this.publisher = publisher;
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
    }

    @Override
    public void processEvent(NotificationEvent event) {
        Optional<Subscription> subscription =
                subscriptionPort.findActiveByClientId(event.getClientId());

        if (subscription.isEmpty() || !subscription.get().accepts(event.getEventType())) {
            event.markSkipped(Instant.now());
            repository.save(event);
            return;
        }

        String webhookUrl = subscription.get().getWebhookUrl();
        try {
            DeliveryResult result = webhookDeliveryPort.deliver(event, webhookUrl);
            Instant attemptedAt = result.attemptedAt();

            if (result.success()) {
                event.markCompleted(attemptedAt);
                repository.save(event);
            } else {
                event.markRetrying(attemptedAt);
                if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                    event.markFailed(attemptedAt);
                    repository.save(event);
                    publisher.publishToDlq(event);
                } else {
                    repository.save(event);
                    publisher.publishForRetry(event);
                }
            }
        } catch (NonRetryableDeliveryException e) {
            Instant now = Instant.now();
            event.markRetrying(now);
            event.markFailed(now);
            repository.save(event);
            publisher.publishToDlq(event);
        }
    }
}
