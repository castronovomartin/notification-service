package com.cobre.notification.adapter.in.rest;

import com.cobre.notification.adapter.in.rest.dto.NotificationEventResponse;
import com.cobre.notification.adapter.in.rest.dto.PagedNotificationEventResponse;
import com.cobre.notification.adapter.in.rest.dto.ReplayResponse;
import com.cobre.notification.adapter.in.rest.mapper.NotificationEventRestMapper;
import com.cobre.notification.domain.model.DeliveryStatus;
import com.cobre.notification.domain.model.NotificationEvent;
import com.cobre.notification.domain.model.NotificationEventFilter;
import com.cobre.notification.domain.model.PageRequest;
import com.cobre.notification.domain.model.PagedResult;
import com.cobre.notification.domain.port.in.NotificationEventUseCase;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notification_events")
public class NotificationEventController {

    private final NotificationEventUseCase useCase;
    private final NotificationEventRestMapper mapper;
    private final RateLimiterRegistry rateLimiterRegistry;

    public NotificationEventController(
            NotificationEventUseCase useCase,
            NotificationEventRestMapper mapper,
            RateLimiterRegistry rateLimiterRegistry) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @GetMapping
    public ResponseEntity<PagedNotificationEventResponse> findAll(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        String clientId = extractClientId(jwt);
        int clampedSize = Math.min(size, 100);

        NotificationEventFilter filter = new NotificationEventFilter(clientId, from, to, status);
        PagedResult<NotificationEvent> result = useCase.findAll(filter, new PageRequest(page, clampedSize));

        List<NotificationEventResponse> content = result.content().stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(new PagedNotificationEventResponse(
                content,
                new PagedNotificationEventResponse.PageMetadata(
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages())));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<NotificationEventResponse> findById(
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {

        String clientId = extractClientId(jwt);
        NotificationEvent event = useCase.findById(eventId, clientId);
        return ResponseEntity.ok(mapper.toResponse(event));
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<ReplayResponse> replay(
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {

        String clientId = extractClientId(jwt);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("replay-endpoint-" + clientId);

        return rateLimiter.executeSupplier(() -> {
            useCase.replay(eventId, clientId);
            return ResponseEntity.accepted()
                    .body(new ReplayResponse(eventId, "PENDING",
                            "Event re-queued for delivery successfully."));
        });
    }

    private String extractClientId(Jwt jwt) {
        String clientId = jwt.getClaimAsString("clientId");
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidTokenException("JWT is missing required claim: clientId");
        }
        return clientId;
    }
}
