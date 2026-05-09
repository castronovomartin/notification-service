# 02 — Domain Model

## Entities

### NotificationEvent

Core domain entity representing a platform-generated event and its
delivery lifecycle. This is the central aggregate of the system.

```java
public class NotificationEvent {
    String eventId;           // Unique identifier. Maps to JSON event_id.
    String eventType;         // e.g. credit_card_payment, debit_transfer
    String content;           // Human-readable event description
    Instant deliveryDate;     // Original event delivery timestamp
    DeliveryStatus status;    // Current delivery status (see enum below)
    String clientId;          // Owner client identifier. Never mutable.
    int retryCount;           // Number of delivery attempts made so far
    Instant lastAttemptAt;    // Timestamp of the most recent attempt
    Instant createdAt;        // Record creation timestamp
    Instant updatedAt;        // Last status update timestamp
}
```

**Invariants:**
- `eventId` is immutable after creation.
- `clientId` is immutable after creation. Never overwritten by request params.
- `retryCount` only increments, never decrements.
- `status` transitions follow the allowed state machine (see below).
- `lastAttemptAt` is updated on every delivery attempt regardless of outcome.

---

### DeliveryStatus

```java
public enum DeliveryStatus {
    PENDING,    // Event received, awaiting first delivery attempt
    RETRYING,   // At least one attempt failed, retry scheduled
    COMPLETED,  // Webhook responded with HTTP 2xx. Terminal state.
    FAILED,     // All retry attempts exhausted. Published to DLQ. Terminal state.
    SKIPPED     // No matching subscription found. Terminal state.
}
```

**Valid state transitions:**

```
PENDING   → COMPLETED   (first attempt succeeded)
PENDING   → RETRYING    (first attempt failed)
PENDING   → SKIPPED     (no active subscription found)
RETRYING  → COMPLETED   (retry attempt succeeded)
RETRYING  → RETRYING    (retry attempt failed, attempts remaining)
RETRYING  → FAILED      (retry attempt failed, no attempts remaining)
FAILED    → PENDING     (client triggered replay via API)
```

**Invalid transitions (must throw DomainException):**
- COMPLETED → any state (terminal, immutable)
- SKIPPED → any state (terminal, immutable)
- FAILED → COMPLETED (replay resets to PENDING, never directly to COMPLETED)

---

### Subscription

Represents a client's webhook registration and event type preferences.

```java
public class Subscription {
    String subscriptionId;      // Unique identifier
    String clientId;            // Associated client. Immutable.
    String webhookUrl;          // HTTPS URL for delivery. Must start with https://
    Set<String> eventTypes;     // Subscribed event types. Empty set = all types
    boolean active;             // Only active subscriptions trigger delivery
    Instant createdAt;
    Instant updatedAt;
}
```

**Invariants:**
- `webhookUrl` must use HTTPS scheme. HTTP is rejected.
- `clientId` is immutable after creation.
- A `clientId` may have at most one active subscription at a time.
- If `eventTypes` is empty, all event types are delivered (wildcard).

---

### NotificationEventFilter

Value object used to filter notification events in query operations.
All fields are optional. Null field means no filter applied for that dimension.

```java
public class NotificationEventFilter {
    String clientId;          // Filter by exact clientId match
    Instant from;             // Filter events with deliveryDate >= from
    Instant to;               // Filter events with deliveryDate <= to
    DeliveryStatus status;    // Filter by exact status match
}
```

**Validation rules:**
- If both `from` and `to` are provided, `from` must be before `to`.
- `clientId` in the filter is always overridden by the authenticated
  client's JWT claim. A client cannot query another client's events.

---

### DeliveryResult

Value object representing the outcome of a single webhook delivery attempt.
Returned by the WebhookDeliveryPort after each attempt.

```java
public class DeliveryResult {
    boolean success;          // True if HTTP 2xx received
    int httpStatusCode;       // Actual HTTP status code received. 0 if no response.
    String errorMessage;      // Populated on failure. Null on success.
    Duration responseTime;    // Time elapsed waiting for webhook response
    Instant attemptedAt;      // Timestamp of this specific attempt
}
```

---

## Business Rules and Invariants

### Delivery rules
1. A notification is only delivered if an active subscription exists
   for the event's `clientId` AND the event type matches the
   subscription's `eventTypes` (or `eventTypes` is empty).

2. Each delivery attempt has a maximum timeout of 5 seconds.
   Exceeding the timeout is treated as a failure.

3. Only HTTP 2xx responses are considered successful delivery.
   HTTP 3xx, 4xx, and 5xx are all treated as failures and trigger retry.

4. The maximum number of automatic retry attempts is 5.
   After exhaustion, the event moves to FAILED. No further automatic
   retry is performed.

5. Manual replay is only allowed on events with status FAILED.
   Attempting to replay a COMPLETED or SKIPPED event returns 400.

### Data integrity rules
6. `clientId` extracted from the JWT must match the `clientId` of any
   event being accessed. Mismatch returns 403, never 404
   (to avoid information leakage about event existence).

7. All timestamps are stored and returned in UTC (Instant / ISO-8601).

8. `retryCount` reflects the number of failed attempts only.
   A successful first attempt results in retryCount = 0, status = COMPLETED.

### State machine enforcement
9. The domain service must validate every status transition before
   applying it. Invalid transitions throw `InvalidStatusTransitionException`
   which maps to HTTP 400.