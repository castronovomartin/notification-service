package com.cobre.notification.domain.model;

import com.cobre.notification.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class DeliveryStatusTransitionTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static NotificationEvent pendingEvent() {
        return new NotificationEvent(
                "evt-001", "credit_card_payment", "Payment received",
                Instant.parse("2026-01-01T00:00:00Z"),
                DeliveryStatus.PENDING,
                "client-1",
                0, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static NotificationEvent eventWithStatus(DeliveryStatus status) {
        return new NotificationEvent(
                "evt-001", "credit_card_payment", "Payment received",
                Instant.parse("2026-01-01T00:00:00Z"),
                status,
                "client-1",
                0, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final Instant T = Instant.parse("2026-05-09T10:00:00Z");

    // -----------------------------------------------------------------------
    // DeliveryStatus.canTransitionTo — valid paths
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canTransitionTo — valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("PENDING -> COMPLETED")
        void pending_to_completed() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("PENDING -> RETRYING")
        void pending_to_retrying() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.RETRYING)).isTrue();
        }

        @Test
        @DisplayName("PENDING -> SKIPPED")
        void pending_to_skipped() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.SKIPPED)).isTrue();
        }

        @Test
        @DisplayName("RETRYING -> COMPLETED")
        void retrying_to_completed() {
            assertThat(DeliveryStatus.RETRYING.canTransitionTo(DeliveryStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("RETRYING -> RETRYING")
        void retrying_to_retrying() {
            assertThat(DeliveryStatus.RETRYING.canTransitionTo(DeliveryStatus.RETRYING)).isTrue();
        }

        @Test
        @DisplayName("RETRYING -> FAILED")
        void retrying_to_failed() {
            assertThat(DeliveryStatus.RETRYING.canTransitionTo(DeliveryStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("FAILED -> PENDING (replay)")
        void failed_to_pending() {
            assertThat(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.PENDING)).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // DeliveryStatus.canTransitionTo — invalid paths
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canTransitionTo — invalid transitions")
    class InvalidTransitions {

        @ParameterizedTest(name = "COMPLETED -> {0}")
        @EnumSource(DeliveryStatus.class)
        @DisplayName("COMPLETED is terminal — no outgoing transitions")
        void completed_is_terminal(DeliveryStatus next) {
            assertThat(DeliveryStatus.COMPLETED.canTransitionTo(next)).isFalse();
        }

        @ParameterizedTest(name = "SKIPPED -> {0}")
        @EnumSource(DeliveryStatus.class)
        @DisplayName("SKIPPED is terminal — no outgoing transitions")
        void skipped_is_terminal(DeliveryStatus next) {
            assertThat(DeliveryStatus.SKIPPED.canTransitionTo(next)).isFalse();
        }

        @Test
        @DisplayName("FAILED -> COMPLETED is forbidden (must go via PENDING)")
        void failed_to_completed_is_forbidden() {
            assertThat(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.COMPLETED)).isFalse();
        }

        @Test
        @DisplayName("FAILED -> RETRYING is forbidden")
        void failed_to_retrying_is_forbidden() {
            assertThat(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.RETRYING)).isFalse();
        }

        @Test
        @DisplayName("FAILED -> FAILED is forbidden")
        void failed_to_failed_is_forbidden() {
            assertThat(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("FAILED -> SKIPPED is forbidden")
        void failed_to_skipped_is_forbidden() {
            assertThat(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.SKIPPED)).isFalse();
        }

        @Test
        @DisplayName("PENDING -> FAILED is forbidden")
        void pending_to_failed_is_forbidden() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("PENDING -> PENDING is forbidden")
        void pending_to_pending_is_forbidden() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.PENDING)).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // NotificationEvent behaviour methods — happy paths
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("NotificationEvent behaviour — valid transitions")
    class EventBehaviourValid {

        @Test
        @DisplayName("PENDING -> RETRYING increments retryCount and records attempt timestamp")
        void markRetrying_fromPending() {
            NotificationEvent event = pendingEvent();

            event.markRetrying(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
            assertThat(event.getRetryCount()).isEqualTo(1);
            assertThat(event.getLastAttemptAt()).isEqualTo(T);
            assertThat(event.getUpdatedAt()).isEqualTo(T);
        }

        @Test
        @DisplayName("RETRYING -> RETRYING increments retryCount again")
        void markRetrying_fromRetrying() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.RETRYING);

            event.markRetrying(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
            assertThat(event.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("PENDING -> COMPLETED records attempt timestamp")
        void markCompleted_fromPending() {
            NotificationEvent event = pendingEvent();

            event.markCompleted(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(event.getLastAttemptAt()).isEqualTo(T);
            assertThat(event.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("RETRYING -> COMPLETED records attempt timestamp")
        void markCompleted_fromRetrying() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.RETRYING);

            event.markCompleted(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
        }

        @Test
        @DisplayName("RETRYING -> FAILED records attempt timestamp")
        void markFailed_fromRetrying() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.RETRYING);

            event.markFailed(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(event.getLastAttemptAt()).isEqualTo(T);
        }

        @Test
        @DisplayName("PENDING -> SKIPPED does not update lastAttemptAt")
        void markSkipped_fromPending() {
            NotificationEvent event = pendingEvent();

            event.markSkipped(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
            assertThat(event.getLastAttemptAt()).isNull();
            assertThat(event.getUpdatedAt()).isEqualTo(T);
        }

        @Test
        @DisplayName("FAILED -> PENDING (replay) resets retryCount and does not change lastAttemptAt")
        void resetForReplay_fromFailed() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.FAILED);

            event.resetForReplay(T);

            assertThat(event.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(event.getRetryCount()).isEqualTo(0);
            assertThat(event.getUpdatedAt()).isEqualTo(T);
            assertThat(event.getLastAttemptAt()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // NotificationEvent behaviour methods — invalid transitions throw
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("NotificationEvent behaviour — invalid transitions throw InvalidStatusTransitionException")
    class EventBehaviourInvalid {

        @Test
        @DisplayName("COMPLETED.markRetrying throws")
        void markRetrying_fromCompleted_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.COMPLETED);
            assertThatThrownBy(() -> event.markRetrying(T))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("RETRYING");
        }

        @Test
        @DisplayName("COMPLETED.markFailed throws")
        void markFailed_fromCompleted_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.COMPLETED);
            assertThatThrownBy(() -> event.markFailed(T))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("SKIPPED.markCompleted throws")
        void markCompleted_fromSkipped_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.SKIPPED);
            assertThatThrownBy(() -> event.markCompleted(T))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("SKIPPED")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("SKIPPED.markRetrying throws")
        void markRetrying_fromSkipped_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.SKIPPED);
            assertThatThrownBy(() -> event.markRetrying(T))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("FAILED.markCompleted throws — replay must go through PENDING")
        void markCompleted_fromFailed_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.FAILED);
            assertThatThrownBy(() -> event.markCompleted(T))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("FAILED")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("PENDING.markFailed throws — must retry before failing")
        void markFailed_fromPending_throws() {
            NotificationEvent event = pendingEvent();
            assertThatThrownBy(() -> event.markFailed(T))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("COMPLETED.resetForReplay throws")
        void resetForReplay_fromCompleted_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.COMPLETED);
            assertThatThrownBy(() -> event.resetForReplay(T))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("SKIPPED.resetForReplay throws")
        void resetForReplay_fromSkipped_throws() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.SKIPPED);
            assertThatThrownBy(() -> event.resetForReplay(T))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("PENDING.resetForReplay throws")
        void resetForReplay_fromPending_throws() {
            NotificationEvent event = pendingEvent();
            assertThatThrownBy(() -> event.resetForReplay(T))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    // -----------------------------------------------------------------------
    // InvalidStatusTransitionException carries correct from/to
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("InvalidStatusTransitionException carries from/to fields")
    class ExceptionFields {

        @Test
        @DisplayName("exception records the from and to states")
        void exception_records_from_and_to() {
            NotificationEvent event = eventWithStatus(DeliveryStatus.COMPLETED);

            InvalidStatusTransitionException ex = catchThrowableOfType(
                    () -> event.markRetrying(T),
                    InvalidStatusTransitionException.class);

            assertThat(ex.getFrom()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(ex.getTo()).isEqualTo(DeliveryStatus.RETRYING);
        }
    }
}
