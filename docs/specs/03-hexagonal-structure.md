# 03 — Hexagonal Architecture Structure

## Core Principle

The domain layer is the center of the application. It has zero knowledge
of infrastructure, frameworks, or delivery mechanisms. All dependencies
point inward: adapters depend on the domain, never the other way around.

```
[REST Controller]  →  [Use Case Port In]  →  [Domain Service]
                                                      ↓
[Kafka Consumer]   →  [Use Case Port In]  →  [Domain Service]
                                                      ↓
                                          [Repository Port Out]
                                                      ↓
                                          [JPA Adapter / Kafka Adapter]
```

---

## Layer Responsibilities

### Domain Layer (`domain/`)
The heart of the application. Contains all business logic, rules,
and invariants. Must be testable with plain JUnit — no Spring context,
no database, no HTTP.

**Allowed imports:** Java standard library only (`java.*`).
**Forbidden imports:** Spring, JPA, Kafka, Jackson, or any framework.

Subpackages:
- `domain/model/` — Entities and value objects. Pure Java classes/records.
- `domain/port/in/` — Use case interfaces. Define what the application can do.
- `domain/port/out/` — Driven port interfaces. Define what the application needs.
- `domain/service/` — Implementations of use case interfaces.
- `domain/exception/` — Custom domain exceptions extending RuntimeException.

### Adapter Layer (`adapter/`)
Implements the ports defined by the domain. Each adapter translates
between the domain model and the external technology it wraps.

**Allowed imports:** Domain ports and models, plus the specific
technology the adapter wraps (Spring MVC, JPA, Kafka, WebClient).
**Forbidden imports:** Other adapters. Adapters never talk to each other.

### Config Layer (`config/`)
Spring `@Configuration` classes that wire adapters to ports.
Contains bean definitions, security config, Kafka config, and
Resilience4j config.

**Allowed imports:** Spring framework, adapters, domain ports.
**Forbidden imports:** Domain service implementations directly
(use port interfaces instead).

---

## Ports

### Driving Ports (in) — What the application exposes

```java
// domain/port/in/NotificationEventUseCase.java
public interface NotificationEventUseCase {

    // Returns paginated events filtered by the provided criteria.
    // clientId in filter is always enforced from JWT — never from raw input.
    Page<NotificationEvent> findAll(NotificationEventFilter filter,
                                    int page, int size);

    // Returns a single event. Throws EventNotFoundException if not found.
    // Throws UnauthorizedAccessException if clientId does not match JWT.
    NotificationEvent findById(String eventId, String authenticatedClientId);

    // Validates event is FAILED, resets to PENDING, publishes to Kafka.
    // Throws ReplayNotAllowedException if status is not FAILED.
    // Returns immediately — delivery is asynchronous.
    void replay(String eventId, String authenticatedClientId);

    // Processes an incoming platform event end-to-end:
    // subscription check → delivery → persist result.
    void processEvent(NotificationEvent event);
}
```

### Driven Ports (out) — What the application needs

```java
// domain/port/out/NotificationEventRepository.java
public interface NotificationEventRepository {
    Page<NotificationEvent> findByFilter(NotificationEventFilter filter,
                                          int page, int size);
    Optional<NotificationEvent> findById(String eventId);
    NotificationEvent save(NotificationEvent event);
}

// domain/port/out/WebhookDeliveryPort.java
public interface WebhookDeliveryPort {
    // Executes a single delivery attempt. Does NOT handle retry.
    // Retry is orchestrated by the domain service via Resilience4j.
    DeliveryResult deliver(NotificationEvent event, String webhookUrl);
}

// domain/port/out/SubscriptionPort.java
public interface SubscriptionPort {
    // Returns empty if no active subscription exists for this clientId.
    Optional<Subscription> findActiveByClientId(String clientId);
}

// domain/port/out/NotificationEventPublisher.java
public interface NotificationEventPublisher {
    void publishToPending(NotificationEvent event);
    void publishToDlq(NotificationEvent event);
}
```

---

## Adapters

### Input Adapters (in)

**`adapter/in/rest/NotificationEventController`**
- Technology: Spring MVC (`@RestController`)
- Receives HTTP requests, extracts JWT claims, calls use case port.
- Maps domain objects to response DTOs using `NotificationEventMapper`.
- Never contains business logic. Only translation and delegation.
- Handles exceptions via `GlobalExceptionHandler` (`@RestControllerAdvice`).

**`adapter/in/messaging/NotificationEventConsumer`**
- Technology: Spring Kafka (`@KafkaListener`)
- Listens to `notifications.pending` topic.
- Deserializes Kafka message to domain `NotificationEvent`.
- Calls `NotificationEventUseCase.processEvent()`.
- Never contains delivery or retry logic.

### Output Adapters (out)

**`adapter/out/persistence/NotificationEventPersistenceAdapter`**
- Technology: Spring Data JPA
- Implements `NotificationEventRepository` port.
- Maps between `NotificationEvent` (domain) and
  `NotificationEventJpaEntity` (JPA) using `NotificationEventJpaMapper`.
- Never exposes JPA entities outside this package.

**`adapter/out/webhook/WebhookDeliveryAdapter`**
- Technology: Spring WebClient (reactive, used in blocking mode)
- Implements `WebhookDeliveryPort`.
- Executes HTTPS POST with 5-second timeout.
- Returns `DeliveryResult` — success or failure details.
- Does NOT implement retry. Retry is Resilience4j's responsibility,
  configured around calls to this adapter.

**`adapter/out/messaging/KafkaNotificationPublisher`**
- Technology: Spring Kafka (`KafkaTemplate`)
- Implements `NotificationEventPublisher` port.
- Publishes events to `notifications.pending` and `notifications.dlq`.
- Partition key is always `clientId`.

---

## Mapping Rules

### Three distinct object types — never mix them

| Type | Location | Purpose | Annotations allowed |
|---|---|---|---|
| Domain entity | `domain/model/` | Business logic carrier | None |
| JPA entity | `adapter/out/persistence/` | Database mapping | `@Entity`, `@Table`, `@Column` etc. |
| REST DTO | `adapter/in/rest/` | API request/response | `@JsonProperty`, `@NotNull` etc. |

### Mapping responsibility
- `NotificationEventJpaMapper` — maps between domain entity and JPA entity.
  Lives in `adapter/out/persistence/`.
- `NotificationEventRestMapper` — maps between domain entity and REST DTOs.
  Lives in `adapter/in/rest/`.
- Mappers are plain Java classes with static or instance methods.
  No MapStruct — explicit mapping only, for full visibility.

---

## Permitted vs Forbidden — Quick Reference

### Domain layer
```
✅ ALLOWED
import java.time.Instant;
import java.util.Optional;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.port.out.NotificationEventRepository;

❌ FORBIDDEN
import org.springframework.stereotype.Service;        // except @Service on service class
import jakarta.persistence.Entity;
import org.apache.kafka.clients.producer.KafkaProducer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.cobre.notification.adapter.out.persistence.NotificationEventJpaEntity;
```

### Adapter layer
```
✅ ALLOWED
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.Entity;

❌ FORBIDDEN
import com.cobre.notification.domain.service.NotificationEventService; // use port, not impl
import com.cobre.notification.adapter.out.persistence.NotificationEventPersistenceAdapter;
import com.cobre.notification.adapter.in.rest.NotificationEventController;
```

### Config layer
```
✅ ALLOWED
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import com.cobre.notification.adapter.in.rest.NotificationEventController;
import com.cobre.notification.adapter.out.persistence.NotificationEventPersistenceAdapter;
import org.springframework.context.annotation.Configuration;

❌ FORBIDDEN
import com.cobre.notification.domain.service.NotificationEventService; // wire via port
```