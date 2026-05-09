# 04 — API Contracts

## Authentication

All endpoints require a valid JWT Bearer token except `GET /actuator/health`.
The `clientId` claim is extracted from the JWT and used for all authorization
checks. It is never accepted as a request parameter.

```
Authorization: Bearer <jwt-token>
```

---

## Endpoints

### GET /notification_events

Returns a paginated list of notification events belonging to the
authenticated client. Supports optional filters.

**Authentication:** Required
**Authorization:** Returns only events where `clientId` matches JWT claim.

#### Request parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `from` | ISO-8601 datetime | No | Filter events with `deliveryDate` >= `from` |
| `to` | ISO-8601 datetime | No | Filter events with `deliveryDate` <= `to` |
| `status` | String (enum) | No | Filter by status: `PENDING`, `RETRYING`, `COMPLETED`, `FAILED`, `SKIPPED` |
| `page` | Integer | No | Page number, zero-based. Default: `0` |
| `size` | Integer | No | Page size. Default: `20`. Max: `100` |

#### Validation rules
- If `from` and `to` are both provided, `from` must be before `to`.
  Otherwise returns `400`.
- `status` must match a valid `DeliveryStatus` value.
  Invalid value returns `400`.
- `size` exceeding `100` is clamped to `100` silently.

#### Response 200 — OK

```json
{
  "content": [
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
    },
    {
      "eventId": "EVT002",
      "eventType": "debit_card_withdrawal",
      "content": "ATM withdrawal of $200.00",
      "deliveryDate": "2024-03-15T10:15:45Z",
      "status": "COMPLETED",
      "clientId": "CLIENT001",
      "retryCount": 0,
      "lastAttemptAt": "2024-03-15T10:15:46Z",
      "createdAt": "2024-03-15T10:15:45Z",
      "updatedAt": "2024-03-15T10:15:46Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1
  }
}
```

#### Example requests

```bash
# All events for authenticated client
curl -X GET "http://localhost:8080/notification_events" \
  -H "Authorization: Bearer <jwt>"

# Filter by status FAILED
curl -X GET "http://localhost:8080/notification_events?status=FAILED" \
  -H "Authorization: Bearer <jwt>"

# Filter by date range
curl -X GET "http://localhost:8080/notification_events\
?from=2024-03-15T09:00:00Z&to=2024-03-15T12:00:00Z" \
  -H "Authorization: Bearer <jwt>"

# Paginated, page 1, 10 per page
curl -X GET "http://localhost:8080/notification_events?page=1&size=10" \
  -H "Authorization: Bearer <jwt>"
```

---

### GET /notification_events/{notification_event_id}

Returns the full details of a single notification event.

**Authentication:** Required
**Authorization:** Event `clientId` must match JWT claim. Returns `403` if not.

#### Path parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `notification_event_id` | String | Yes | The unique event identifier |

#### Response 200 — OK

```json
{
  "eventId": "EVT003",
  "eventType": "credit_transfer",
  "content": "Bank transfer received from Account #4567 for $1,500.00",
  "deliveryDate": "2024-03-15T11:20:18Z",
  "status": "FAILED",
  "clientId": "CLIENT002",
  "retryCount": 5,
  "lastAttemptAt": "2024-03-15T11:21:50Z",
  "createdAt": "2024-03-15T11:20:18Z",
  "updatedAt": "2024-03-15T11:21:50Z"
}
```

#### Example requests

```bash
# Get event by ID
curl -X GET "http://localhost:8080/notification_events/EVT003" \
  -H "Authorization: Bearer <jwt-client002>"

# Attempting to access another client's event
curl -X GET "http://localhost:8080/notification_events/EVT001" \
  -H "Authorization: Bearer <jwt-client002>"
# → 403 Forbidden
```

---

### POST /notification_events/{notification_event_id}/replay

Triggers asynchronous re-delivery of a failed notification event.
The event is re-queued in Kafka and processed by the dispatcher.
Returns immediately without waiting for delivery outcome.

**Authentication:** Required
**Authorization:** Event `clientId` must match JWT claim. Returns `403` if not.
**Rate limiting:** Maximum 10 requests per minute per `clientId`.

#### Path parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `notification_event_id` | String | Yes | The unique event identifier |

#### Request body
None.

#### Response 202 — Accepted

```json
{
  "eventId": "EVT003",
  "status": "PENDING",
  "message": "Event re-queued for delivery successfully."
}
```

#### Example requests

```bash
# Replay a failed event
curl -X POST \
  "http://localhost:8080/notification_events/EVT003/replay" \
  -H "Authorization: Bearer <jwt-client002>"
# → 202 Accepted

# Attempting to replay a completed event
curl -X POST \
  "http://localhost:8080/notification_events/EVT001/replay" \
  -H "Authorization: Bearer <jwt-client001>"
# → 400 Bad Request
```

---

## Error Responses

All error responses follow this structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable description of the error",
  "path": "/notification_events/EVT001/replay",
  "timestamp": "2024-03-15T09:30:22Z"
}
```

### Error catalog

| HTTP Status | Code | Cause |
|---|---|---|
| `400` | `INVALID_STATUS_TRANSITION` | Replay attempted on non-FAILED event |
| `400` | `INVALID_DATE_RANGE` | `from` is after `to` in filter |
| `400` | `INVALID_STATUS_VALUE` | `status` param does not match enum values |
| `401` | `UNAUTHORIZED` | Missing or invalid JWT token |
| `403` | `FORBIDDEN` | Event `clientId` does not match authenticated client |
| `404` | `EVENT_NOT_FOUND` | No event found with the given ID |
| `429` | `RATE_LIMIT_EXCEEDED` | More than 10 replay requests per minute |
| `500` | `INTERNAL_ERROR` | Unexpected server error |

#### 400 — Bad Request example

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Replay is not allowed for events with status COMPLETED.",
  "path": "/notification_events/EVT001/replay",
  "timestamp": "2024-03-15T09:30:22Z"
}
```

#### 403 — Forbidden example

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied to the requested resource.",
  "path": "/notification_events/EVT003",
  "timestamp": "2024-03-15T11:20:18Z"
}
```

#### 404 — Not Found example

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Notification event with id EVT999 was not found.",
  "path": "/notification_events/EVT999",
  "timestamp": "2024-03-15T11:20:18Z"
}
```

---

## Pagination Behavior

- Pagination is zero-based: `page=0` returns the first page.
- Default page size is `20` if not specified.
- Maximum page size is `100`. Larger values are silently clamped.
- Response always includes the `page` metadata block with
  `number`, `size`, `totalElements`, and `totalPages`.
- Requesting a page beyond `totalPages` returns an empty `content`
  array with `200 OK`, never a `404`.
- Default sort order is `deliveryDate` descending (most recent first).