# 01 — System Design

## Overview

The Notification Service is a cloud-native, event-driven microservice
built for Cobre's transactional platform. It handles two distinct
capabilities:

**Subsystem 1 — Event Notification Delivery**
Listens to platform-generated events, validates client subscriptions,
and delivers notifications to client-registered webhook URLs via HTTPS.
Handles failures with an exponential backoff retry strategy and
persists the final delivery state for every event.

**Subsystem 2 — Self-Service API**
Exposes a REST API for clients to query their notification history,
inspect individual events, and trigger manual replay of failed
deliveries.

---

## Components and Responsibilities

| Component | Type | Responsibility |
|---|---|---|
| Platform Core | External | Generates financial events (payments, transfers, etc.) |
| Subscription Service | Internal | Stores client webhook URLs and active event type subscriptions |
| Kafka Event Bus | Infrastructure | Decouples event production from delivery. Topics: `notifications.pending`, `notifications.retry`, `notifications.dlq` |
| Notification Dispatcher | Domain Service | Consumes pending events, validates subscription, delegates to webhook delivery |
| Webhook Delivery Adapter | Output Adapter | Executes HTTPS POST to client webhook URL with timeout and retry |
| Retry Engine | Infrastructure | Resilience4j exponential backoff with jitter. Max 5 attempts |
| Dead Letter Queue | Infrastructure | Kafka topic receiving events that exhausted all retry attempts |
| Notification Store | Output Adapter | Persists every event and its final delivery status in PostgreSQL |
| Self-Service API | Input Adapter | Spring MVC REST controller exposing query and replay endpoints |
| Observability Stack | Infrastructure | Micrometer metrics, OpenTelemetry traces, structured logs with MDC |

---

## Notification Delivery Flow

```
1. Platform Core publishes a financial event to Kafka topic
   `notifications.pending`, partitioned by clientId.

2. Notification Dispatcher consumes the event.

3. Dispatcher queries Subscription Service:
   - If no active subscription exists for this clientId → discard event,
     persist with status SKIPPED.
   - If subscription exists but event type is not subscribed → discard,
     persist with status SKIPPED.
   - If subscription is active and event type matches → proceed.

4. Dispatcher calls Webhook Delivery Adapter with the event payload
   and the registered webhook URL.

5. Webhook Delivery Adapter executes HTTPS POST to the webhook URL
   with a 5-second timeout.

6. On HTTP 2xx response:
   - Event status updated to COMPLETED.
   - Persisted in Notification Store.
   - Micrometer counter `notifications.delivered` incremented.

7. On failure (timeout, non-2xx, connection error):
   - Retry Engine applies exponential backoff with jitter.
   - Event status updated to RETRYING.
   - Retry attempt count incremented.
```

---

## Retry Flow

```
Attempt 1 fails → wait ~1s (±50% jitter)
Attempt 2 fails → wait ~2s (±50% jitter)
Attempt 3 fails → wait ~4s (±50% jitter)
Attempt 4 fails → wait ~8s (±50% jitter)
Attempt 5 fails → publish to `notifications.dlq`
               → event status updated to FAILED
               → persisted in Notification Store
               → Micrometer counter `notifications.failed` incremented

Client can trigger manual replay via:
POST /notification_events/{id}/replay
→ validates event status is FAILED
→ resets status to PENDING
→ publishes to `notifications.pending`
→ returns 202 Accepted immediately
```

---

## Acceptance Criteria

### Subscription validation

```
Given a platform event is received for clientId CLIENT001
When CLIENT001 has no active subscription
Then the event is discarded and persisted with status SKIPPED
And no webhook delivery is attempted

Given a platform event is received for clientId CLIENT001
When CLIENT001 has an active subscription for event type credit_transfer
And the received event type is debit_purchase
Then the event is discarded and persisted with status SKIPPED

Given a platform event is received for clientId CLIENT001
When CLIENT001 has an active subscription matching the event type
Then the notification delivery is triggered to the registered webhook URL
```

### Webhook delivery

```
Given a notification delivery is triggered
When the webhook URL responds with HTTP 200
Then the event status is updated to COMPLETED
And the delivery is persisted in the Notification Store

Given a notification delivery is triggered
When the webhook URL responds with HTTP 500
Then the Retry Engine schedules a retry with exponential backoff
And the event status is updated to RETRYING

Given a notification has been retried 5 times
When the 5th attempt also fails
Then the event is published to notifications.dlq
And the event status is updated to FAILED
And no further automatic retry is attempted
```

### Self-service API

```
Given a client is authenticated with a valid JWT
When they request GET /notification_events
Then only events belonging to their clientId are returned
And results are paginated

Given a client requests GET /notification_events/{id}
When the event belongs to a different clientId
Then the API returns 403 Forbidden

Given a client requests POST /notification_events/{id}/replay
When the event status is FAILED
Then the event is re-queued and 202 Accepted is returned

Given a client requests POST /notification_events/{id}/replay
When the event status is COMPLETED
Then the API returns 400 Bad Request
```

---

## Non-Functional Requirements

### Scalability
- The Notification Dispatcher must be horizontally scalable.
  Multiple instances consume from Kafka consumer groups without
  duplicate delivery.
- Kafka partitioning by `clientId` guarantees ordered processing
  per client across multiple dispatcher instances.
- The Self-Service API is stateless and horizontally scalable
  behind a load balancer.

### Resilience
- No single point of failure. Kafka decouples producers from consumers.
  If the dispatcher is down, events accumulate in Kafka and are
  processed when it recovers.
- Webhook delivery failures never block event processing for other
  clients. Each event is processed independently.
- The DLQ ensures no event is permanently lost without explicit
  operator action.
- Circuit breaker pattern available via Resilience4j to prevent
  cascading failures when a webhook endpoint is consistently down.

### Observability
- Every delivery attempt emits structured logs with `correlationId`
  and `clientId` in MDC context.
- Prometheus metrics available at `/actuator/prometheus`.
- Key alerting metrics:
    - `notifications.failed` rate > 10% over 5-minute window per client
    - `notifications.delivery.duration` p99 > 2 seconds
    - DLQ topic lag > 0 (any event in DLQ triggers alert)