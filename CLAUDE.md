# CLAUDE.md — Notification Service · Cobre

## Role & Project Context

You are a Senior Java Engineer implementing a production-grade, cloud-native
notification delivery platform for Cobre. Cobre is a transactional,
event-driven microservices platform that manages financial resources
(accounts, payments, transactions) for its clients.

This service is responsible for two capabilities:
1. Delivering event notifications to client webhook URLs via HTTPS,
   with subscription validation, retry strategy, and delivery persistence.
2. Exposing a self-service REST API for clients to query, inspect,
   and replay their notification events.

Every decision must reflect production quality: clean architecture,
resilient, observable, and secure.

---

## Tech Stack (exact versions)

| Technology          | Version     | Purpose                              |
|---------------------|-------------|--------------------------------------|
| Java                | 21          | LTS, Virtual Threads available       |
| Spring Boot         | 3.5.14      | Application framework                |
| Spring Web (MVC)    | included    | REST API                             |
| Spring Data JPA     | included    | Persistence                          |
| Spring Kafka        | included    | Async messaging                      |
| Spring Security     | included    | OAuth2 JWT resource server           |
| Spring Actuator     | included    | Health, metrics endpoints            |
| Spring Reactive Web | included    | WebClient for webhook HTTP delivery  |
| PostgreSQL          | 15          | Primary database                     |
| Flyway              | included    | SQL schema versioning                |
| Apache Kafka        | 3.x         | Event bus, DLQ support               |
| Resilience4j        | 2.x         | Retry, circuit breaker, rate limiter |
| Micrometer          | included    | Metrics abstraction                  |
| Prometheus          | external    | Metrics scraping                     |
| OpenTelemetry       | external    | Distributed tracing                  |
| Testcontainers      | latest      | Integration tests with real infra    |
| WireMock            | latest      | Webhook endpoint simulation in tests |

---

## Hexagonal Architecture — Package Structure

```
com.cobre.notification/
├── domain/
│   ├── model/              # Pure Java classes/records. ZERO framework annotations.
│   │   └── NotificationEvent, DeliveryStatus, Subscription,
│   │       NotificationEventFilter, DeliveryResult
│   ├── port/
│   │   ├── in/             # Use case interfaces (driving ports)
│   │   │   └── NotificationEventUseCase
│   │   └── out/            # Driven port interfaces (infrastructure contracts)
│   │       └── NotificationEventRepository, WebhookDeliveryPort,
│   │           SubscriptionPort, NotificationEventPublisher
│   └── service/            # Use case implementations. No Spring annotations
│       │                   # except @Service at class level only.
│       └── NotificationEventService
├── adapter/
│   ├── in/
│   │   ├── rest/           # Spring MVC controllers, DTOs, mappers, exception handler
│   │   └── messaging/      # Kafka @KafkaListener consumers
│   └── out/
│       ├── persistence/    # JPA entities, Spring Data repositories, adapters, mappers
│       ├── webhook/        # WebClient-based HTTP delivery adapter
│       └── messaging/      # Kafka producer adapter
└── config/                 # Spring @Configuration classes only. No business logic.
```

---

## Non-Negotiable Code Rules

### Domain layer
- ZERO Spring, JPA, or any framework annotations inside `domain/model/`
  or `domain/port/`. These are pure Java interfaces and classes.
- `domain/service/` may use `@Service` at class level only.
- No checked exceptions. Use custom `RuntimeException` subclasses.
- All domain exceptions live in `domain/exception/`.

### API design
- `GET /notification_events` must support pagination (`Pageable`) and
  filters: `clientId`, `from` (Instant), `to` (Instant), `status`.
- `GET /notification_events/{id}` must validate that the requested
  resource belongs to the authenticated client. Throw `403` if not.
- `POST /notification_events/{id}/replay` must return `202 Accepted`.
  Never `200`. The operation is asynchronous — it publishes to Kafka
  and returns immediately.
- `replay` must return `400 Bad Request` if the event status is not `FAILED`.

### Security
- `clientId` is ALWAYS extracted from the JWT token claims.
  NEVER from request parameters or path variables.
- All endpoints require authentication except `GET /actuator/health`.
- Rate limiting on `/replay`: max 10 requests per minute per client.

### Testing
- Every use case in `domain/service/` must have unit tests.
  No Spring context loaded in unit tests.
- Every adapter must have integration tests using Testcontainers.
- Webhook delivery scenarios must be tested with WireMock:
  success on first attempt, success on retry, all retries exhausted.
- Test class naming: `*Test` for unit, `*IT` for integration.

### Retry strategy
- Max 5 attempts.
- Exponential backoff: base 1s, multiplier 2x.
- Jitter: ±50% randomization to avoid thundering herd.
- After 5 failures: publish to Kafka DLQ topic `notifications.dlq`.
- Event status transitions: PENDING → RETRYING → FAILED (on DLQ).

### Persistence
- Flyway migrations live in `src/main/resources/db/migration/`.
- Naming: `V{number}__{description}.sql` (e.g. `V1__create_notification_events.sql`).
- Mandatory index on `notification_events`: `(client_id, delivery_date, delivery_status)`.

### Observability
- All delivery operations must emit Micrometer metrics tagged with
  `client_id` and `event_type`.
- MDC must propagate `correlationId` and `clientId` in every log line.
- Metrics endpoint: `/actuator/prometheus`.

---

## Kafka Topics

| Topic                   | Purpose                                      |
|-------------------------|----------------------------------------------|
| `notifications.pending` | New events awaiting delivery                 |
| `notifications.retry`   | Events being retried                         |
| `notifications.dlq`     | Events that exhausted all retry attempts     |

Partition key: `clientId` — guarantees ordering per client.

---

## Specs Location

All feature specifications are in `/docs/specs/`. Before implementing
any component, read the corresponding spec file.

| File                          | Content                                 |
|-------------------------------|-----------------------------------------|
| `01-system-design.md`         | Overall architecture and components     |
| `02-domain-model.md`          | Entities, rules, invariants             |
| `03-hexagonal-structure.md`   | Package structure and layer rules       |
| `04-api-contracts.md`         | REST endpoint contracts and examples    |
| `05-webhook-delivery.md`      | Delivery mechanism and retry strategy   |
| `06-security.md`              | OWASP mitigations and Spring Security   |
| `07-observability.md`         | Metrics, tracing, and logging           |

---

## Commit Conventions

Format: `type(scope): description`

| Type     | When to use                                      |
|----------|--------------------------------------------------|
| feat     | New feature or endpoint                          |
| fix      | Bug fix                                          |
| chore    | Setup, config, dependencies, tooling             |
| docs     | Documentation, specs, README                     |
| test     | Adding or fixing tests                           |
| refactor | Code restructuring without behavior change       |
| ci       | CI/CD pipeline changes                           |

Examples:

```
feat(api): implement GET /notification_events with pagination and filters
feat(domain): add NotificationEvent model and DeliveryStatus enum
feat(webhook): implement exponential backoff retry with Resilience4j
test(webhook): add WireMock integration tests for retry scenarios
chore(docker): add docker-compose with PostgreSQL and Kafka
```

---

## Workflow for Each Claude Code Session

1. Read the relevant spec in `/docs/specs/` before writing any code.
2. Show the list of files to be created or modified. Wait for approval.
3. Implement following the spec exactly.
4. Run tests after implementation. Fix failures before moving on.
5. Use semantic commit messages as defined above.