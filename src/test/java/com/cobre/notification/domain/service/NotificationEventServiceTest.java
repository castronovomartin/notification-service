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
import com.cobre.notification.domain.port.out.NotificationEventPublisher;
import com.cobre.notification.domain.port.out.NotificationEventRepository;
import com.cobre.notification.domain.port.out.SubscriptionPort;
import com.cobre.notification.domain.port.out.WebhookDeliveryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock private NotificationEventRepository repository;
    @Mock private WebhookDeliveryPort webhookDeliveryPort;
    @Mock private SubscriptionPort subscriptionPort;
    @Mock private NotificationEventPublisher publisher;
    @Spy  private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks private NotificationEventService service;

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private static final String EVENT_ID      = "evt-001";
    private static final String CLIENT_ID     = "client-001";
    private static final String OTHER_CLIENT  = "client-999";
    private static final Instant T            = Instant.parse("2026-05-09T10:00:00Z");

    private NotificationEvent eventWith(DeliveryStatus status, int retryCount) {
        return new NotificationEvent(
                EVENT_ID, "credit_card_payment", "Payment received",
                T, status, CLIENT_ID,
                retryCount, retryCount > 0 ? T : null, T, T);
    }

    private NotificationEvent pendingEvent() {
        return eventWith(DeliveryStatus.PENDING, 0);
    }

    private Subscription wildcardSubscription() {
        return new Subscription(
                "sub-001", CLIENT_ID, "https://client.example.com/webhook",
                Set.of(), true, T, T);
    }

    private Subscription subscriptionFor(String... eventTypes) {
        return new Subscription(
                "sub-001", CLIENT_ID, "https://client.example.com/webhook",
                Set.of(eventTypes), true, T, T);
    }

    private DeliveryResult success() {
        return new DeliveryResult(true, 200, null, Duration.ofMillis(150), T);
    }

    private DeliveryResult failure() {
        return new DeliveryResult(false, 500, "Internal Server Error", Duration.ofSeconds(1), T);
    }

    // -----------------------------------------------------------------------
    // findById
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns event when found and clientId matches")
        void happyPath() {
            NotificationEvent event = pendingEvent();
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            NotificationEvent result = service.findById(EVENT_ID, CLIENT_ID);

            assertThat(result).isSameAs(event);
        }

        @Test
        @DisplayName("throws EventNotFoundException when event does not exist")
        void eventNotFound() {
            when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining(EVENT_ID);
        }

        @Test
        @DisplayName("throws UnauthorizedAccessException when clientId does not match event owner")
        void clientIdMismatch() {
            NotificationEvent event = pendingEvent(); // owned by CLIENT_ID
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.findById(EVENT_ID, OTHER_CLIENT))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining(EVENT_ID)
                    .hasMessageContaining(OTHER_CLIENT);
        }
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("delegates to repository with the filter as-is and returns its result")
        void delegatesAndReturns() {
            NotificationEventFilter filter = new NotificationEventFilter(CLIENT_ID, null, null, null);
            PageRequest page = new PageRequest(0, 10);
            PagedResult<NotificationEvent> expected =
                    new PagedResult<>(List.of(pendingEvent()), 1, 0, 10);
            when(repository.findAll(filter, page)).thenReturn(expected);

            PagedResult<NotificationEvent> result = service.findAll(filter, page);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(filter, page);
        }
    }

    // -----------------------------------------------------------------------
    // replay
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("replay")
    class Replay {

        @Test
        @DisplayName("resets FAILED event to PENDING, saves, and publishes for delivery")
        void happyPath() {
            NotificationEvent event = eventWith(DeliveryStatus.FAILED, 5);
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            service.replay(EVENT_ID, CLIENT_ID);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(event.getRetryCount()).isEqualTo(0);
            verify(repository).save(event);
            verify(publisher).publishForDelivery(event);
        }

        @Test
        @DisplayName("throws EventNotFoundException when event does not exist")
        void eventNotFound() {
            when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.replay(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("throws UnauthorizedAccessException when clientId does not match event owner")
        void clientIdMismatch() {
            NotificationEvent event = eventWith(DeliveryStatus.FAILED, 5);
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.replay(EVENT_ID, OTHER_CLIENT))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("throws ReplayNotAllowedException when event is COMPLETED")
        void replayOnCompleted() {
            NotificationEvent event = eventWith(DeliveryStatus.COMPLETED, 0);
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.replay(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(ReplayNotAllowedException.class)
                    .hasMessageContaining("COMPLETED");

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("throws ReplayNotAllowedException when event is RETRYING")
        void replayOnRetrying() {
            NotificationEvent event = eventWith(DeliveryStatus.RETRYING, 2);
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.replay(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(ReplayNotAllowedException.class)
                    .hasMessageContaining("RETRYING");

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("throws ReplayNotAllowedException when event is PENDING")
        void replayOnPending() {
            NotificationEvent event = pendingEvent();
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.replay(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(ReplayNotAllowedException.class)
                    .hasMessageContaining("PENDING");

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("throws ReplayNotAllowedException when event is SKIPPED")
        void replayOnSkipped() {
            NotificationEvent event = eventWith(DeliveryStatus.SKIPPED, 0);
            when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.replay(EVENT_ID, CLIENT_ID))
                    .isInstanceOf(ReplayNotAllowedException.class)
                    .hasMessageContaining("SKIPPED");

            verifyNoInteractions(publisher);
        }
    }

    // -----------------------------------------------------------------------
    // processEvent
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("processEvent")
    class ProcessEvent {

        @Test
        @DisplayName("marks SKIPPED when no active subscription found for clientId")
        void noSubscriptionFound() {
            NotificationEvent event = pendingEvent();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.empty());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
            verify(repository).save(event);
            verifyNoInteractions(webhookDeliveryPort, publisher);
        }

        @Test
        @DisplayName("marks SKIPPED when subscription does not accept the event type")
        void subscriptionRejectsEventType() {
            NotificationEvent event = pendingEvent(); // eventType = credit_card_payment
            Subscription sub = subscriptionFor("debit_transfer"); // different type
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
            verify(repository).save(event);
            verifyNoInteractions(webhookDeliveryPort, publisher);
        }

        @Test
        @DisplayName("marks COMPLETED when delivery succeeds on first attempt")
        void successOnFirstAttempt() {
            NotificationEvent event = pendingEvent();
            Subscription sub = wildcardSubscription();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl())).thenReturn(success());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(event.getRetryCount()).isEqualTo(0);
            assertThat(event.getLastAttemptAt()).isEqualTo(T);
            verify(repository).save(event);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("marks RETRYING and publishes to retry topic when delivery fails with attempts remaining")
        void failureWithRetriesRemaining() {
            NotificationEvent event = pendingEvent(); // retryCount = 0
            Subscription sub = wildcardSubscription();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl())).thenReturn(failure());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
            assertThat(event.getRetryCount()).isEqualTo(1);
            verify(repository).save(event);
            verify(publisher).publishForRetry(event);
            verify(publisher, never()).publishToDlq(any());
        }

        @Test
        @DisplayName("marks FAILED and publishes to DLQ when all 5 attempts are exhausted")
        void allRetriesExhausted() {
            // Event has already been retried 4 times; this 5th attempt also fails
            NotificationEvent event = eventWith(DeliveryStatus.RETRYING, 4);
            Subscription sub = wildcardSubscription();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl())).thenReturn(failure());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(event.getRetryCount()).isEqualTo(5);
            verify(repository).save(event);
            verify(publisher).publishToDlq(event);
            verify(publisher, never()).publishForRetry(any());
        }

        @Test
        @DisplayName("wildcard subscription (empty eventTypes) accepts any event type")
        void wildcardSubscriptionAcceptsAllTypes() {
            NotificationEvent event = pendingEvent();
            Subscription sub = wildcardSubscription(); // empty set = all types
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl())).thenReturn(success());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
        }

        @Test
        @DisplayName("marks FAILED and publishes to DLQ immediately on NonRetryableDeliveryException (4xx)")
        void nonRetryableDelivery_marksFailedAndPublishesToDlq() {
            NotificationEvent event = pendingEvent();
            Subscription sub = wildcardSubscription();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl()))
                    .thenThrow(new NonRetryableDeliveryException(EVENT_ID, 400));

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            verify(repository).save(event);
            verify(publisher).publishToDlq(event);
            verify(publisher, never()).publishForRetry(any());
        }

        @Test
        @DisplayName("delivery succeeds on retry attempt (not first): COMPLETED with retryCount unchanged")
        void successOnRetryAttempt() {
            NotificationEvent event = eventWith(DeliveryStatus.RETRYING, 2);
            Subscription sub = wildcardSubscription();
            when(subscriptionPort.findActiveByClientId(CLIENT_ID)).thenReturn(Optional.of(sub));
            when(webhookDeliveryPort.deliver(event, sub.getWebhookUrl())).thenReturn(success());

            service.processEvent(event);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(event.getRetryCount()).isEqualTo(2); // not incremented on success
            verify(repository).save(event);
            verifyNoInteractions(publisher);
        }
    }
}
