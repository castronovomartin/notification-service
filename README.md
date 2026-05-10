# Notification Service

A production-grade, cloud-native notification delivery platform built for **Cobre**.

The service delivers financial event notifications to client webhook endpoints with
subscription validation, exponential-backoff retry, and a self-service REST API for
querying and replaying events.

---

## Architecture

Hexagonal (Ports & Adapters) architecture with a strict separation between domain
logic and infrastructure concerns.

```
com.cobre.notification/
├── domain/
│   ├── model/          # Pure Java — zero framework annotations
│   ├── port/in/        # Driving ports (use case interfaces)
│   ├── port/out/       # Driven ports (infrastructure contracts)
│   └── service/        # Use case implementations
├── adapter/
│   ├── in/
│   │   ├── rest/       # Spring MVC controllers, DTOs, MDC filter
│   │   └── messaging/  # Kafka @KafkaListener consumers
│   └── out/
│       ├── persistence/ # JPA entities, Spring Data repositories
│       ├── webhook/     # WebClient HTTP delivery adapter
│       ├── messaging/   # Kafka producer adapter
│       └── health/      # Custom health indicators
└── config/             # Spring @Configuration classes only
```

### Delivery flow

```
External system
     │
     ▼
notifications.pending  ──► NotificationEventConsumer
                                    │
                                    ▼
                         NotificationEventService
                          ├── subscription check
                          ├── WebhookDeliveryAdapter (1 attempt)
                          │     ├── 2xx  → COMPLETED
                          │     ├── 5xx  → RETRYING → notifications.retry
                          │     └── 4xx  → FAILED   → notifications.dlq
                          └── max 5 retries (exponential backoff via Kafka)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads ready) |
| Framework | Spring Boot 3.5.14 |
| REST API | Spring Web MVC |
| Async messaging | Apache Kafka 3.x |
| HTTP delivery | Spring WebFlux WebClient (Reactor Netty) |
| Persistence | Spring Data JPA + PostgreSQL 15 |
| Schema migrations | Flyway |
| Security | Spring Security OAuth2 JWT Resource Server |
| Rate limiting | Resilience4j RateLimiter |
| Metrics | Micrometer + Prometheus |
| Structured logging | Logback + logstash-logback-encoder |
| API docs | SpringDoc OpenAPI (Swagger UI) |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

---

## How to Run Locally

### 1. Start the infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL 15** on `localhost:5432` (database: `notification_db`)
- **Zookeeper** on `localhost:2181`
- **Kafka** on `localhost:9092`

Wait for the health checks to pass (about 30 s):

```bash
docker-compose ps
```

### 2. Configure environment variables

```bash
cp .env.example .env
# Edit .env and set JWT_JWK_SET_URI to your authorization server
```

### 3. Run the application

```bash
export $(cat .env | xargs)
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

Flyway applies the schema migration automatically on startup.
The `DataInitializer` seeds 8 sample events from
`src/main/resources/data/notification_events.json`.

### 4. Explore the API

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Prometheus metrics: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)

Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 5. Stop the infrastructure

```bash
docker-compose down          # keep volumes
docker-compose down -v       # destroy volumes (reset all data)
```

---

## Running Tests

```bash
# Unit tests only (no Docker required)
./mvnw test

# Integration tests (requires Docker for Testcontainers)
./mvnw test -Dtest="WebhookDeliveryAdapterIT,NotificationEventControllerIT,NotificationEventPersistenceAdapterIT"

# Full suite
./mvnw test -Dtest="*IT,*Test"
```

Test coverage:
- **Domain** — pure unit tests, no Spring context, 40 tests
- **`NotificationEventService`** — Mockito unit tests, 19 tests
- **`WebhookDeliveryAdapterIT`** — WireMock, 6 scenarios (no Spring context)
- **`NotificationEventPersistenceAdapterIT`** — Testcontainers PostgreSQL, 10 tests
- **`NotificationEventControllerIT`** — `@WebMvcTest` + Spring Security, 14 tests

---

## API Examples

All endpoints require a valid JWT. Replace `<your-jwt-token>` with a token
that contains the `clientId` claim and the appropriate scope.

### List notification events

```bash
curl -s http://localhost:8080/notification_events \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

Filter by status:

```bash
curl -s "http://localhost:8080/notification_events?status=FAILED" \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

Filter by date range (CLIENT001 events on 2024-03-15):

```bash
curl -s "http://localhost:8080/notification_events?from=2024-03-15T00:00:00Z&to=2024-03-15T23:59:59Z" \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

Paginate (page 0, 5 results per page):

```bash
curl -s "http://localhost:8080/notification_events?page=0&size=5" \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

### Get a single event

Retrieve EVT001 (COMPLETED, credit card payment for $150.00):

```bash
curl -s http://localhost:8080/notification_events/EVT001 \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

Expected response:

```json
{
  "eventId": "EVT001",
  "eventType": "credit_card_payment",
  "content": "Credit card payment received for $150.00",
  "deliveryDate": "2024-03-15T09:30:22Z",
  "status": "COMPLETED",
  "clientId": "CLIENT001",
  "retryCount": 0,
  "lastAttemptAt": "2024-03-15T09:30:23Z",
  "createdAt": "2024-03-15T09:30:22Z",
  "updatedAt": "2024-03-15T09:30:23Z"
}
```

### Replay a failed event

EVT003 (FAILED after 5 retries — credit transfer for $1,500.00, CLIENT002):

```bash
curl -s -X POST http://localhost:8080/notification_events/EVT003/replay \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

Expected response (`202 Accepted`):

```json
{
  "eventId": "EVT003",
  "status": "PENDING",
  "message": "Event re-queued for delivery successfully."
}
```

EVT004 is also FAILED and can be replayed (debit transfer for $750.00, CLIENT001):

```bash
curl -s -X POST http://localhost:8080/notification_events/EVT004/replay \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

### Error responses

Replaying a non-FAILED event returns `400 Bad Request`:

```bash
curl -s -X POST http://localhost:8080/notification_events/EVT001/replay \
  -H "Authorization: Bearer <your-jwt-token>" | jq .
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Replay not allowed for event 'EVT001' with status COMPLETED",
  "path": "/notification_events/EVT001/replay",
  "timestamp": "2024-03-15T10:00:00Z"
}
```

Accessing another client's event returns `403 Forbidden`:

```bash
# JWT has clientId=CLIENT001 but EVT003 belongs to CLIENT002
curl -s http://localhost:8080/notification_events/EVT003 \
  -H "Authorization: Bearer <token-for-CLIENT001>" | jq .
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JWT_JWK_SET_URI` | `http://localhost:9000/oauth2/jwks` | JWKS endpoint of the OAuth2 authorization server |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `SPRING_DATASOURCE_URL` | — | JDBC URL (e.g. `jdbc:postgresql://localhost:5432/notification_db`) |
| `SPRING_DATASOURCE_USERNAME` | — | Database username |
| `SPRING_DATASOURCE_PASSWORD` | — | Database password |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile; controls log format and environment metric tag |

---

## Kafka Topics

| Topic | Partitions | Purpose |
|---|---|---|
| `notifications.pending` | 6 | New events awaiting delivery |
| `notifications.retry` | 6 | Events scheduled for retry |
| `notifications.dlq` | 1 | Events that exhausted all 5 retry attempts |

Partition key is always `clientId` — guarantees per-client ordering.

---

## Retry Strategy

- Max **5 attempts** (1 initial + 4 retries)
- Exponential backoff: base **1 s**, multiplier **2×**
- Jitter: **±50%** randomization to avoid thundering herd
- HTTP **4xx** responses are non-retryable — event goes directly to DLQ
- After 5 failures: event status → `FAILED`, published to `notifications.dlq`

---

## Observability

| Signal | Details |
|---|---|
| Metrics | Micrometer → Prometheus at `/actuator/prometheus` |
| Health | `/actuator/health` (Postgres + Kafka + diskSpace) |
| Logs | JSON structured (logstash format) with `correlationId`, `clientId`, `eventId` in every line |
| Tracing | OpenTelemetry auto-instrumentation, 100% sampling rate |

Key metrics emitted:

| Metric | Tags |
|---|---|
| `notifications.delivered` | `client_id`, `event_type` |
| `notifications.failed` | `client_id`, `event_type`, `reason` |
| `notifications.retried` | `client_id`, `event_type` |
| `notifications.skipped` | `client_id`, `event_type` |
| `notifications.replayed` | `client_id` |
| `notifications.delivery.duration` | `client_id`, `event_type`, `outcome` |

---

## Documentation

Full design documentation, architectural decisions, and AI usage log:
[Notion — Notification Service](https://amber-wok-959.notion.site/Sr-Software-Engineer-Challenge-35b1fc046a6b801c9927d09c5ec4956f)

Source code:
[GitHub — castronovomartin/notification-service](https://github.com/castronovomartin/notification-service)
