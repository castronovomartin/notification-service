# 06 — Security

## OWASP Top 10 — Identified Vulnerabilities

### Vulnerability 1 — API1:2023 Broken Object Level Authorization (BOLA)

**Description:**
BOLA occurs when an API endpoint uses user-supplied identifiers to access
objects without verifying that the requesting user owns or is authorized
to access that object.

**Concrete attack vector in this API:**
A malicious client authenticated as CLIENT002 makes the following request:

```
GET /notification_events/EVT001
Authorization: Bearer <valid-jwt-for-CLIENT002>
```

EVT001 belongs to CLIENT001. Without ownership validation, the API would
return CLIENT001's financial transaction data to CLIENT002. In a financial
platform, this exposes sensitive transaction amounts, account numbers,
and payment details of other clients.

**Mitigation implemented:**
`clientId` is extracted exclusively from the JWT token claims. Every
resource access validates that the event's `clientId` matches the
authenticated client's `clientId`. Mismatches return `403 Forbidden`,
never `404` — to avoid confirming the existence of resources belonging
to other clients (information leakage prevention).

```java
// domain/service/NotificationEventService.java
public NotificationEvent findById(String eventId,
                                   String authenticatedClientId) {
    NotificationEvent event = repository.findById(eventId)
        .orElseThrow(() -> new EventNotFoundException(eventId));

    if (!event.getClientId().equals(authenticatedClientId)) {
        throw new UnauthorizedAccessException(
            "Access denied to event " + eventId);
    }
    return event;
}
```

---

### Vulnerability 2 — A03:2021 Injection

**Description:**
Injection flaws occur when untrusted data is sent to an interpreter
as part of a query or command. In REST APIs this manifests as
SQL injection via query parameters used in dynamic queries.

**Concrete attack vector in this API:**
The `GET /notification_events` endpoint accepts `status` and date
parameters. If these are concatenated directly into a JPQL or SQL
string, an attacker could manipulate the query:

```
GET /notification_events?status=COMPLETED' OR '1'='1
```

This could bypass status filtering and return all events regardless
of clientId, exposing the entire notification history of all clients.

**Mitigation implemented:**
All query parameters are bound to Java types before reaching the
persistence layer. Spring's `@RequestParam` with explicit type
conversion rejects invalid values at the controller layer.
Spring Data JPA Specifications use parameterized queries exclusively —
never string concatenation.

```java
// adapter/in/rest/NotificationEventController.java
@GetMapping
public ResponseEntity<PageResponse<NotificationEventResponse>> findAll(
        @RequestParam(required = false) DeliveryStatus status,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size,
        @AuthenticationPrincipal Jwt jwt) {

    String clientId = jwt.getClaimAsString("clientId");
    NotificationEventFilter filter =
        new NotificationEventFilter(clientId, from, to, status);
    return ResponseEntity.ok(
        useCase.findAll(filter, page, size)
               .map(mapper::toResponse));
}

// adapter/out/persistence/NotificationEventSpecification.java
public static Specification<NotificationEventJpaEntity> withFilter(
        NotificationEventFilter filter) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("clientId"),
                                filter.clientId()));
        if (filter.status() != null) {
            predicates.add(cb.equal(root.get("status"),
                                    filter.status()));
        }
        if (filter.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("deliveryDate"), filter.from()));
        }
        if (filter.to() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                root.get("deliveryDate"), filter.to()));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

---

### Vulnerability 3 — A05:2021 Security Misconfiguration

**Description:**
Security misconfiguration includes missing authentication on sensitive
endpoints, overly permissive CORS, exposed actuator endpoints, and
lack of rate limiting on state-changing operations.

**Concrete attack vector in this API:**
The `POST /notification_events/{id}/replay` endpoint without rate
limiting allows an attacker (or a misconfigured client) to trigger
thousands of webhook redeliveries per minute, causing:
- Denial of Service against the client's own webhook server
- Exhaustion of Kafka producer capacity
- Amplified load on the notification dispatcher

Additionally, exposing all Spring Actuator endpoints publicly leaks
internal metrics, heap dumps, and environment variables.

**Mitigation implemented:**
Rate limiting via Resilience4j RateLimiter on the replay endpoint,
restricted Actuator exposure, and explicit Spring Security configuration.

```yaml
# application.yaml
resilience4j:
  rate-limiter:
    instances:
      replay-endpoint:
        limit-for-period: 10
        limit-refresh-period: 1m
        timeout-duration: 0s

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
  endpoint:
    health:
      show-details: never
```

---

## Spring Security Configuration

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(GET, "/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers(GET, "/notification_events/**")
                    .hasAuthority("SCOPE_notification:read")
                .requestMatchers(POST, "/notification_events/*/replay")
                    .hasAuthority("SCOPE_notification:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(
                    new BearerTokenAccessDeniedHandler())
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

---

## JWT Claims Extraction

```java
// adapter/in/rest/JwtClientIdExtractor.java
@Component
public class JwtClientIdExtractor {

    public String extractClientId(Jwt jwt) {
        String clientId = jwt.getClaimAsString("clientId");
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidTokenException(
                "JWT is missing required claim: clientId");
        }
        return clientId;
    }
}
```

The `clientId` claim must be present in every JWT. Its absence results
in `401 Unauthorized`. It is never sourced from request parameters,
path variables, or request body.

---

## Rate Limiting Implementation

```java
// adapter/in/rest/NotificationEventController.java
@PostMapping("/{eventId}/replay")
public ResponseEntity<ReplayResponse> replay(
        @PathVariable String eventId,
        @AuthenticationPrincipal Jwt jwt) {

    String clientId = extractor.extractClientId(jwt);

    RateLimiter rateLimiter = rateLimiterRegistry
        .rateLimiter("replay-endpoint-" + clientId);

    return RateLimiter.decorateSupplier(rateLimiter, () -> {
        useCase.replay(eventId, clientId);
        return ResponseEntity.accepted()
            .body(new ReplayResponse(eventId, "PENDING",
                "Event re-queued for delivery successfully."));
    }).get();
}
```

Rate limiting is applied per `clientId`, not globally. This prevents
one misbehaving client from affecting others.

---

## Security Test Requirements

### Authentication tests
```
Given a request with no Authorization header
When any protected endpoint is called
Then 401 Unauthorized is returned

Given a request with an expired JWT
When any protected endpoint is called
Then 401 Unauthorized is returned

Given a request with a valid JWT missing the clientId claim
When any protected endpoint is called
Then 401 Unauthorized is returned
```

### Authorization tests
```
Given CLIENT002 is authenticated
When GET /notification_events/EVT001 is requested (owned by CLIENT001)
Then 403 Forbidden is returned
And the response body does not reveal EVT001 existence

Given CLIENT001 is authenticated
When GET /notification_events is requested
Then only CLIENT001 events are returned
And CLIENT002 events are never included
```

### Rate limiting tests
```
Given CLIENT001 sends 10 replay requests within 1 minute
When the 11th replay request is sent
Then 429 Too Many Requests is returned

Given CLIENT001 is rate limited
When CLIENT002 sends a replay request
Then CLIENT002 request succeeds normally
```

### Input validation tests
```
Given status parameter value is "INVALID_STATUS"
When GET /notification_events?status=INVALID_STATUS is called
Then 400 Bad Request is returned

Given from parameter is after to parameter
When GET /notification_events?from=2024-03-15&to=2024-03-01 is called
Then 400 Bad Request is returned
```