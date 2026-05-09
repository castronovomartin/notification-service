# 07 — Observability

## Overview

The notification service must be observable in near real-time to allow
the internal monitoring team to detect delivery anomalies and respond
promptly to client complaints. Observability is implemented across
three pillars: metrics (Micrometer + Prometheus), traces (OpenTelemetry
+ Jaeger), and structured logs (Logback + MDC).

---

## Micrometer Metrics

### Counters

| Metric name | Tags | Description |
|---|---|---|
| `notifications.delivered` | `client_id`, `event_type` | Incremented on every successful delivery |
| `notifications.failed` | `client_id`, `event_type`, `reason` | Incremented when event reaches DLQ |
| `notifications.retried` | `client_id`, `event_type` | Incremented on every retry attempt |
| `notifications.skipped` | `client_id`, `event_type` | Incremented when no subscription matches |
| `notifications.replayed` | `client_id` | Incremented on every replay request |

### Timers

| Metric name | Tags | Description |
|---|---|---|
| `notifications.delivery.duration` | `client_id`, `event_type`, `outcome` | End-to-end delivery time per attempt |
| `notifications.webhook.response.time` | `client_id`, `http_status` | HTTP response time from webhook endpoint |

### Gauges

| Metric name | Tags | Description |
|---|---|---|
| `notifications.pending.count` | — | Current number of events in PENDING status |
| `notifications.retrying.count` | — | Current number of events in RETRYING status |
| `notifications.dlq.lag` | — | Number of unprocessed messages in DLQ topic |

### Implementation

```java
// domain/service/NotificationEventService.java
@Service
public class NotificationEventService implements NotificationEventUseCase {

    private final MeterRegistry meterRegistry;

    private void recordDeliverySuccess(NotificationEvent event) {
        meterRegistry.counter("notifications.delivered",
            "client_id", event.getClientId(),
            "event_type", event.getEventType()
        ).increment();

        meterRegistry.timer("notifications.delivery.duration",
            "client_id", event.getClientId(),
            "event_type", event.getEventType(),
            "outcome", "success"
        ).record(Duration.between(event.getCreatedAt(), Instant.now()));
    }

    private void recordDeliveryFailure(NotificationEvent event,
                                        String reason) {
        meterRegistry.counter("notifications.failed",
            "client_id", event.getClientId(),
            "event_type", event.getEventType(),
            "reason", reason
        ).increment();

        meterRegistry.timer("notifications.delivery.duration",
            "client_id", event.getClientId(),
            "event_type", event.getEventType(),
            "outcome", "failure"
        ).record(Duration.between(event.getCreatedAt(), Instant.now()));
    }

    private void recordRetryAttempt(NotificationEvent event) {
        meterRegistry.counter("notifications.retried",
            "client_id", event.getClientId(),
            "event_type", event.getEventType()
        ).increment();
    }
}
```

---

## MDC Context Propagation

Every log line must carry `correlationId` and `clientId` in the MDC
context to enable log correlation across distributed components.

### MDC Filter

```java
// adapter/in/rest/MdcContextFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        try {
            String correlationId = Optional
                .ofNullable(((HttpServletRequest) request)
                    .getHeader("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());

            MDC.put("correlationId", correlationId);
            MDC.put("service", "notification-service");

            ((HttpServletResponse) response)
                .setHeader("X-Correlation-Id", correlationId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### ClientId MDC Enrichment

```java
// adapter/in/rest/NotificationEventController.java
private void enrichMdc(String clientId) {
    MDC.put("clientId", clientId);
}
```

Called immediately after JWT extraction in every controller method.
Cleared automatically by `MdcContextFilter` at request completion.

### Kafka Consumer MDC Propagation

```java
// adapter/in/messaging/NotificationEventConsumer.java
@KafkaListener(topics = "notifications.pending",
               groupId = "notification-dispatcher")
public void consume(ConsumerRecord<String, NotificationEvent> record) {
    try {
        MDC.put("correlationId", UUID.randomUUID().toString());
        MDC.put("clientId", record.key());
        MDC.put("eventId", record.value().getEventId());
        MDC.put("topic", record.topic());
        MDC.put("partition", String.valueOf(record.partition()));

        useCase.processEvent(record.value());
    } finally {
        MDC.clear();
    }
}
```

---

## Structured Log Format

All logs must be emitted in JSON format using Logback with
`logstash-logback-encoder`.

### logback-spring.xml

```xml
<configuration>
  <appender name="JSON_CONSOLE"
            class="ch.qos.logback.core.ConsoleAppender">
    <encoder
      class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>correlationId</includeMdcKeyName>
      <includeMdcKeyName>clientId</includeMdcKeyName>
      <includeMdcKeyName>eventId</includeMdcKeyName>
      <includeMdcKeyName>service</includeMdcKeyName>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON_CONSOLE"/>
  </root>
</configuration>
```

### Expected log output examples

**Successful delivery:**
```json
{
  "timestamp": "2024-03-15T09:30:23.456Z",
  "level": "INFO",
  "logger": "c.c.n.domain.service.NotificationEventService",
  "message": "Notification delivered successfully",
  "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "clientId": "CLIENT001",
  "eventId": "EVT001",
  "eventType": "credit_card_payment",
  "httpStatus": 200,
  "durationMs": 342,
  "service": "notification-service"
}
```

**Retry attempt:**
```json
{
  "timestamp": "2024-03-15T11:20:19.123Z",
  "level": "WARN",
  "logger": "c.c.n.adapter.out.webhook.WebhookDeliveryAdapter",
  "message": "Webhook delivery attempt failed, scheduling retry",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "clientId": "CLIENT002",
  "eventId": "EVT003",
  "attemptNumber": 2,
  "httpStatus": 500,
  "nextRetryInMs": 1847,
  "service": "notification-service"
}
```

**Event sent to DLQ:**
```json
{
  "timestamp": "2024-03-15T11:21:51.789Z",
  "level": "ERROR",
  "logger": "c.c.n.domain.service.NotificationEventService",
  "message": "All retry attempts exhausted, publishing to DLQ",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "clientId": "CLIENT002",
  "eventId": "EVT003",
  "totalAttempts": 5,
  "service": "notification-service"
}
```

---

## Prometheus and Actuator Configuration

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info
      base-path: /actuator
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
    prometheus:
      enabled: true
  metrics:
    tags:
      application: notification-service
      environment: ${SPRING_PROFILES_ACTIVE:local}
    distribution:
      percentiles-histogram:
        notifications.delivery.duration: true
        notifications.webhook.response.time: true
      percentiles:
        notifications.delivery.duration: 0.5, 0.95, 0.99
        notifications.webhook.response.time: 0.5, 0.95, 0.99
```

---

## Key Alerts

| Alert name | Condition | Window | Severity |
|---|---|---|---|
| `HighDeliveryFailureRate` | `notifications.failed` rate > 10% of total per client | 5 minutes | Critical |
| `SlowWebhookDelivery` | `notifications.delivery.duration` p99 > 2s | 5 minutes | Warning |
| `DlqNotEmpty` | `notifications.dlq.lag` > 0 | Immediate | Critical |
| `HighRetryRate` | `notifications.retried` rate > 50% of delivered | 10 minutes | Warning |
| `ServiceDown` | `GET /actuator/health` returns non-200 | 1 minute | Critical |

---

## OpenTelemetry Distributed Tracing

```yaml
# application.yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

Trace propagation is automatic via Spring Boot Actuator tracing.
Every inbound HTTP request and Kafka message creates a new span.
The `correlationId` from MDC is injected into the trace as a
custom attribute for cross-system correlation.

---

## Health Indicators

```java
// adapter/out/messaging/KafkaHealthIndicator.java
@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Health health() {
        try {
            kafkaAdmin.describeTopics("notifications.pending");
            return Health.up()
                .withDetail("topic", "notifications.pending")
                .withDetail("status", "reachable")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

**Required health indicators:**

| Indicator | Checks | Down condition |
|---|---|---|
| `db` | PostgreSQL connectivity | Cannot execute simple query |
| `kafka` | Kafka broker reachability | Cannot describe topic |
| `diskSpace` | Available disk | Less than 10MB free |