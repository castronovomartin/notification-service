# 05 — Webhook Delivery

## Overview

The webhook delivery mechanism is responsible for executing HTTPS POST
requests to client-registered webhook URLs. It is deliberately simple:
one attempt, one result. Retry orchestration is handled externally by
Resilience4j, which wraps calls to the delivery adapter.

---

## WebClient Configuration

```yaml
# application.yaml
webhook:
  client:
    connect-timeout-ms: 3000
    response-timeout-ms: 5000
    max-in-memory-size-mb: 1
```

```java
// config/WebClientConfig.java
@Bean
public WebClient webhookWebClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(5))
        .secure(ssl -> ssl.sslContext(
            SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
        ));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE,
                       MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-Cobre-Source", "notification-service")
        .defaultHeader("X-Cobre-Version", "1.0")
        .codecs(config -> config.defaultCodecs()
            .maxInMemorySize(1024 * 1024))
        .build();
}
```

**Request headers sent on every delivery attempt:**

| Header | Value | Purpose |
|---|---|---|
| `Content-Type` | `application/json` | Payload format |
| `X-Cobre-Source` | `notification-service` | Identifies sender |
| `X-Cobre-Version` | `1.0` | API version for client verification |
| `X-Cobre-Event-Id` | `{eventId}` | Idempotency key for client deduplication |
| `X-Cobre-Client-Id` | `{clientId}` | Client identifier |

---

## Delivery Payload

```json
{
  "eventId": "EVT003",
  "eventType": "credit_transfer",
  "content": "Bank transfer received from Account #4567 for $1,500.00",
  "deliveryDate": "2024-03-15T11:20:18Z",
  "clientId": "CLIENT002",
  "retryCount": 0,
  "deliveredAt": "2024-03-15T11:20:19Z"
}
```

---

## Resilience4j Retry Configuration

```yaml
# application.yaml
resilience4j:
  retry:
    instances:
      webhook-delivery:
        max-attempts: 5
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        randomized-wait-factor: 0.5
        retry-on-result-predicate: >
          com.cobre.notification.adapter.out.webhook.NonSuccessResultPredicate
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.reactive.function.client.WebClientRequestException
        ignore-exceptions:
          - com.cobre.notification.domain.exception.NonRetryableDeliveryException
```

**Parameter explanation:**

| Parameter | Value | Meaning |
|---|---|---|
| `max-attempts` | `5` | Maximum total attempts including the first |
| `wait-duration` | `1s` | Base wait time before first retry |
| `exponential-backoff-multiplier` | `2` | Each wait doubles the previous |
| `randomized-wait-factor` | `0.5` | ±50% jitter applied to each wait |
| `retry-on-result-predicate` | custom | Retries on non-2xx HTTP responses |
| `retry-exceptions` | IO/Timeout/WebClient | Retries on connection-level failures |
| `ignore-exceptions` | NonRetryable | Skips retry for permanent failures |

**Wait times per attempt (approximate with jitter):**

| Attempt | Base wait | With jitter range |
|---|---|---|
| 1 (initial) | — | immediate |
| 2 | 1s | 0.5s — 1.5s |
| 3 | 2s | 1.0s — 3.0s |
| 4 | 4s | 2.0s — 6.0s |
| 5 | 8s | 4.0s — 12.0s |
| After 5 | — | → DLQ |

---

## Delivery Flow Per Attempt

```
1. Domain service calls WebhookDeliveryAdapter.deliver(event, webhookUrl)
   wrapped in Resilience4j Retry decorator.

2. WebhookDeliveryAdapter executes:
   - HTTPS POST to webhookUrl
   - Payload: JSON-serialized event
   - Headers: Content-Type, X-Cobre-* headers
   - Timeout: 5 seconds response, 3 seconds connect

3. Outcome evaluation:
   a. HTTP 2xx received:
      → DeliveryResult(success=true, httpStatusCode=200, ...)
      → Resilience4j stops retrying
      → Domain service updates status to COMPLETED
      → Persists to Notification Store

   b. HTTP 3xx/4xx/5xx received:
      → DeliveryResult(success=false, httpStatusCode=500, ...)
      → NonSuccessResultPredicate returns true
      → Resilience4j schedules retry with backoff
      → Domain service updates status to RETRYING

   c. Timeout (connect or response):
      → WebClientRequestException thrown
      → Resilience4j catches, schedules retry with backoff
      → Domain service updates status to RETRYING

   d. All 5 attempts exhausted:
      → Resilience4j throws MaxRetriesExceededException
      → Domain service catches, updates status to FAILED
      → Publishes event to notifications.dlq via NotificationEventPublisher
      → Persists final FAILED state to Notification Store
```

---

## Dead Letter Queue Behavior

The DLQ (`notifications.dlq`) is a Kafka topic that receives events
that have exhausted all retry attempts.

**On DLQ publish:**
- Event status is set to `FAILED`.
- `retryCount` reflects the total number of failed attempts (5).
- `lastAttemptAt` is updated to the timestamp of the final attempt.
- No automatic processing occurs from the DLQ.

**Manual recovery:**
- A client calls `POST /notification_events/{id}/replay`.
- The domain service validates status is `FAILED`.
- Status is reset to `PENDING`, `retryCount` is NOT reset
  (preserved for audit purposes).
- Event is published to `notifications.pending`.
- The full delivery flow restarts from attempt 1.

---

## Kafka Configuration

```yaml
# application.yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: notification-dispatcher
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "com.cobre.notification.domain.model"
```

**Topic configuration:**

| Topic | Partitions | Replication | Retention | Purpose |
|---|---|---|---|---|
| `notifications.pending` | 6 | 1 (dev) / 3 (prod) | 7 days | Awaiting delivery |
| `notifications.retry` | 6 | 1 (dev) / 3 (prod) | 7 days | Scheduled retries |
| `notifications.dlq` | 1 | 1 (dev) / 3 (prod) | 30 days | Failed events |

**Partition key:** `clientId` — guarantees all events for a given
client are processed in order by the same consumer instance.

**Consumer group:** `notification-dispatcher` — enables horizontal
scaling. Multiple dispatcher instances share the load without
duplicate processing.

---

## WireMock Test Scenarios

The following scenarios must be covered by integration tests
using `WireMockServer` via `wiremock-spring-boot`.

### Scenario 1 — Success on first attempt
```
Given webhook server returns HTTP 200
When deliver() is called
Then DeliveryResult.success is true
And retryCount remains 0
And event status is COMPLETED
```

### Scenario 2 — Success on third attempt
```
Given webhook server returns HTTP 500 twice then HTTP 200
When deliver() is called with Resilience4j retry
Then DeliveryResult.success is true after 3 attempts
And retryCount is 2
And event status is COMPLETED
```

### Scenario 3 — All retries exhausted
```
Given webhook server always returns HTTP 500
When deliver() is called with Resilience4j retry
Then MaxRetriesExceededException is thrown after 5 attempts
And retryCount is 5
And event status is FAILED
And event is published to notifications.dlq
```

### Scenario 4 — Connection timeout
```
Given webhook server delays response beyond 5 seconds
When deliver() is called
Then TimeoutException triggers retry
And retry backoff is applied
```

### Scenario 5 — Webhook URL unreachable
```
Given webhook server is not running
When deliver() is called
Then WebClientRequestException triggers retry immediately
```

### Scenario 6 — HTTP 400 from webhook (non-retryable)
```
Given webhook server returns HTTP 400
When deliver() is called
Then NonRetryableDeliveryException is thrown
And Resilience4j does NOT retry
And event status is FAILED immediately
```